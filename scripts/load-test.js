import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 100,
    duration: '1m',
    gracefulStop: '30s',
    thresholds: {
        http_req_failed: ['rate<0.05'], // Error rate < 5%
        http_req_duration: ['p(95)<1500', 'avg<800'], // p95 < 1500ms, avg < 800ms
    },
};

const BASE_URL = __ENV.BACKEND_URL || 'http://127.0.0.1:5000';

export default function () {
    // Define the primary endpoints to test
    const endpoints = [
        { method: 'GET', url: '/api/health', expectedStatus: 200 },
        { method: 'POST', url: '/api/login', body: JSON.stringify({ email: 'test@example.com', password: 'password123' }), expectedStatus: 401 }, // Mocked for test
        { method: 'POST', url: '/api/register', body: JSON.stringify({ email: 'new@example.com', password: 'pass' }), expectedStatus: 400 },
        { method: 'POST', url: '/api/predict', body: JSON.stringify({ image_data: 'base64...' }), expectedStatus: 401 },
        { method: 'GET', url: '/api/profile', expectedStatus: 401 },
        { method: 'GET', url: '/api/dashboard', expectedStatus: 401 },
        { method: 'GET', url: '/api/history', expectedStatus: 401 }
    ];

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // Iterate through endpoints and execute requests
    for (const endpoint of endpoints) {
        let res;
        if (endpoint.method === 'GET') {
            res = http.get(`${BASE_URL}${endpoint.url}`, params);
        } else if (endpoint.method === 'POST') {
            res = http.post(`${BASE_URL}${endpoint.url}`, endpoint.body, params);
        }

        console.log(`Request: ${endpoint.method} ${BASE_URL}${endpoint.url} -> ${res.status}`);
        if (res.body) {
            console.log(String(res.body).substring(0, 300));
        }

        // Validate response
        check(res, {
            'status is 200 or expected': (r) => r.status === endpoint.expectedStatus || r.status === 200 || r.status === 201 || r.status === 401,
            'response body is not empty': (r) => r.body.length > 0,
            'response time is < 2000ms': (r) => r.timings.duration < 2000,
        });

        // Small wait between endpoint requests to mimic real user interaction flow
        sleep(0.5);
    }
}
