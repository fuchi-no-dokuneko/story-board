(function exposeStoryBlockAuth(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) {
    module.exports = api;
  } else {
    root.StoryBlockAuth = api;
  }
}(typeof globalThis === 'undefined' ? this : globalThis, function createStoryBlockAuth() {
  'use strict';

  function codedError(code, message) {
    const error = new Error(message);
    error.code = code;
    return error;
  }

  function normalizedToken(token) {
    const value = String(token || '').trim();
    if (/\p{Cc}/u.test(value)) {
      throw codedError('INVALID_BEARER_TOKEN', 'The bearer token contains invalid control characters.');
    }
    return value;
  }

  function authorizationHeaders(token) {
    const value = normalizedToken(token);
    return value ? { Authorization: `Bearer ${value}` } : {};
  }

  function resolveSameOriginUrl(input, origin) {
    const value = String(input || '').trim();
    if (!value) {
      throw codedError('INVALID_REQUEST_TARGET', 'Enter a request path.');
    }
    const base = new URL(origin);
    const target = new URL(value, base);
    if (target.origin !== base.origin || target.username || target.password) {
      throw codedError(
        'CROSS_ORIGIN_REQUEST',
        'Only requests to this StoryBlock server are allowed; the bearer token was not sent.',
      );
    }
    return target.href;
  }

  return Object.freeze({ authorizationHeaders, resolveSameOriginUrl });
}));
