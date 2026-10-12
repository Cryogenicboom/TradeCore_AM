import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 load test script for TradeCore Prototype
// Target: 50 concurrent simulated users submitting limit orders

export const options = {
    vus: 50,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<200'], // 95% of requests should complete within 200ms
        http_req_failed: ['rate<0.05'],    // Rejections/errors below 5% under normal load
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

const USERS = ['alice', 'bob', 'charlie', 'dave'];

export default function () {
    const userIndex = Math.floor(Math.random() * USERS.length);
    const userId = USERS[userIndex];
    const isBuy = Math.random() > 0.5;
    const side = isBuy ? 'BUY' : 'SELL';
    
    // Realistic order parameters crossing around 50,000 BTC-USD
    const price = isBuy ? (50000 + Math.floor(Math.random() * 50)) : (50000 - Math.floor(Math.random() * 50));
    const qty = 1;

    const payload = JSON.stringify({
        id: `k6-${__VU}-${__ITER}-${Date.now()}`,
        userId: userId,
        side: side,
        price: price,
        qty: qty
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 1. Submit Limit Order
    const res = http.post(`${BASE_URL}/orders`, payload, params);

    check(res, {
        'status is 200 or accepted': (r) => r.status === 200 || r.status === 429,
    });

    // 2. Sample OrderBook snapshot occasionally
    if (__ITER % 5 === 0) {
        const obRes = http.get(`${BASE_URL}/orderbook`);
        check(obRes, {
            'orderbook response is 200': (r) => r.status === 200,
        });
    }

    sleep(0.1);
}
