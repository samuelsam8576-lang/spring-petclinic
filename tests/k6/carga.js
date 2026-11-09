// CNF01 - Teste de carga (RNF01): 50 usuários por 3 minutos sustentados;
// 4 min 30 s no total com as rampas.
// Execução: k6 run tests/k6/carga.js
// Estresse: k6 run --stage 2m:200 --stage 2m:200 --stage 30s:0 tests/k6/carga.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '1m', target: 50 }, // rampa até 50 usuários
    { duration: '3m', target: 50 }, // carga sustentada
    { duration: '30s', target: 0 }, // desaceleração
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const html = { headers: { Accept: 'text/html' } };
  const respostas = [
    http.get(`${BASE}/`, html),
    http.get(`${BASE}/owners?lastName=Davis`, html),
    http.get(`${BASE}/owners/1`, html),
    http.get(`${BASE}/vets.html`, html),
    http.get(`${BASE}/vets`, { headers: { Accept: 'application/json' } }),
  ];
  respostas.forEach((r) => check(r, { 'status 200': (res) => res.status === 200 }));

  // 10% das iterações cadastram um proprietário, para exercitar a escrita no banco
  if (Math.random() < 0.1) {
    const r = http.post(`${BASE}/owners/new`, {
      firstName: 'Carga',
      lastName: `K6${__VU}x${__ITER}`,
      address: 'Rua Teste, 1',
      city: 'Cruzeiro',
      telephone: '1234567890',
    });
    check(r, { 'cadastro ok': (res) => res.status === 200 });
  }
  sleep(1);
}
