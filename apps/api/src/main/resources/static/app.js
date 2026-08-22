const method = document.querySelector('#method');
const path = document.querySelector('#path');
const token = document.querySelector('#token');
const body = document.querySelector('#body');
const response = document.querySelector('#response');
const responseStatus = document.querySelector('#response-status');
const statusDot = document.querySelector('#status-dot');
const statusText = document.querySelector('#status-text');

async function send() {
  const headers = { Accept: 'application/json, application/yaml, text/plain' };
  const credential = token.value.trim();
  if (credential) headers.Authorization = `Bearer ${credential}`;
  const options = { method: method.value, headers };
  if (!['GET', 'HEAD'].includes(method.value)) {
    headers['Content-Type'] = 'application/json';
    options.body = body.value;
  }
  response.textContent = 'Loading...';
  responseStatus.textContent = '';
  try {
    const result = await fetch(path.value.trim(), options);
    const text = await result.text();
    responseStatus.textContent = `${result.status} ${result.statusText}`;
    try {
      response.textContent = JSON.stringify(JSON.parse(text), null, 2);
    } catch {
      response.textContent = text || '(empty response)';
    }
  } catch (error) {
    responseStatus.textContent = 'Network error';
    response.textContent = error.message;
  }
}

document.querySelector('#send').addEventListener('click', send);
document.querySelectorAll('nav button').forEach((button) => {
  button.addEventListener('click', () => {
    method.value = button.dataset.method;
    path.value = button.dataset.path;
    send();
  });
});

fetch('/actuator/health')
  .then((result) => result.json())
  .then((health) => {
    const up = health.status === 'UP';
    statusDot.className = up ? 'up' : 'down';
    statusText.textContent = up ? 'API online' : 'API degraded';
  })
  .catch(() => {
    statusDot.className = 'down';
    statusText.textContent = 'API unavailable';
  });
