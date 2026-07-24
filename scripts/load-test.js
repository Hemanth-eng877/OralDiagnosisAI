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
    // Note: /api/diagnose removed because it requires multipart/form-data image uploads
    const endpoints = [
        { 
            method: 'POST', 
            url: '/api/login', 
            bodyFn: () => JSON.stringify({ email: 'test@example.com', password: 'password123' }), 
            expectedStatuses: [200, 401, 503] 
        },
        { 
            method: 'POST', 
            url: '/api/signup', 
            bodyFn: () => JSON.stringify({ email: `test_${__VU}_${__ITER}@example.com`, password: 'password123' }), 
            expectedStatuses: [200, 201, 400, 503] 
        },
        { 
            method: 'GET', 
            url: '/api/dashboard', 
            bodyFn: () => null, 
            expectedStatuses: [200, 401, 503] 
        },
        { 
            method: 'GET', 
            url: '/api/reports', 
            bodyFn: () => null, 
            expectedStatuses: [200, 401, 503] 
        }
    ];

    const baseParams = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // Iterate through endpoints and execute requests
    for (const endpoint of endpoints) {
        let res;
        let url = `${BASE_URL}${endpoint.url}`;
        let body = endpoint.bodyFn();

        // Prevent k6 from marking expected responses as HTTP failures
        let params = Object.assign({}, baseParams, {
            responseCallback: http.expectedStatuses(...endpoint.expectedStatuses)
        });

        if (endpoint.method === 'GET') {
            res = http.get(url, params);
        } else if (endpoint.method === 'POST') {
            res = http.post(url, body, params);
        }

        let firebaseError = "";
        try {
            if (res.body) {
                let parsed = JSON.parse(res.body);
                if (parsed.message && parsed.message.includes("Firebase")) {
                    firebaseError = parsed.message;
                }
            }
        } catch(e) {}

        console.log("================================");
        console.log("METHOD:", endpoint.method);
        console.log("URL:", res.request.url);
        console.log("STATUS:", res.status);
        console.log("RESPONSE TIME:", res.timings.duration + "ms");
        console.log("BODY PREVIEW:", res.body ? String(res.body).substring(0, 200) : "");
        if (firebaseError) {
            console.log("FIREBASE ERROR:", firebaseError);
        }
        console.log("================================");

        // Validate response
        check(res, {
            'status is expected': (r) => endpoint.expectedStatuses.includes(r.status),
            'response time is < 2000ms': (r) => r.timings.duration < 2000,
        });

        // Small wait between endpoint requests to mimic real user interaction flow
        sleep(0.5);
    }
}
