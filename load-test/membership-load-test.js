import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Full membership lifecycle per iteration: subscribe -> inquiry -> payment -> benefits -> cancel
// Run: docker run --rm -v <project>/load-test:/scripts grafana/k6 run /scripts/membership-load-test.js

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000/api/v1';

export const options = {
    scenarios: {
        ramping_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },  // ramp-up
                { duration: '60s', target: 50 },  // sustained load
                { duration: '30s', target: 100 }, // spike
                { duration: '15s', target: 0 },   // ramp-down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        business_errors: ['rate<0.01'],
    },
};

const businessErrors = new Rate('business_errors');
const HEADERS = { 'Content-Type': 'application/json' };
const GRADES = ['BASIC', 'PREMIUM'];

export default function () {
    // Unique user per iteration so the ACTIVE-membership invariant is never violated
    const userId = __VU * 1000000 + __ITER;
    const grade = GRADES[userId % GRADES.length];

    // 1. Subscribe
    const subscribeRes = http.post(
        `${BASE_URL}/memberships`,
        JSON.stringify({ user_id: userId, grade: grade }),
        { headers: HEADERS, tags: { name: 'subscribe' } },
    );
    const subscribed = check(subscribeRes, {
        'subscribe 201': (r) => r.status === 201,
    });
    businessErrors.add(!subscribed);
    if (!subscribed) return;

    // 2. Get membership
    const getRes = http.get(`${BASE_URL}/memberships/${userId}`, { tags: { name: 'get-membership' } });
    businessErrors.add(!check(getRes, { 'get membership 200': (r) => r.status === 200 }));

    // 3. Process payment
    const payRes = http.post(
        `${BASE_URL}/payments`,
        JSON.stringify({ user_id: userId, payment_method: 'CARD' }),
        { headers: HEADERS, tags: { name: 'payment' } },
    );
    businessErrors.add(!check(payRes, { 'payment ok': (r) => r.status === 200 || r.status === 201 }));

    // 4. Get benefits
    const benefitRes = http.get(`${BASE_URL}/benefits/${userId}`, { tags: { name: 'benefits' } });
    businessErrors.add(!check(benefitRes, { 'benefits 200': (r) => r.status === 200 }));

    // 5. Cancel (keeps load realistic and DB from accumulating ACTIVE rows only)
    const cancelRes = http.del(`${BASE_URL}/memberships/${userId}`, null, { tags: { name: 'cancel' } });
    businessErrors.add(!check(cancelRes, { 'cancel ok': (r) => r.status === 200 || r.status === 204 }));

    sleep(0.3); // think time
}
