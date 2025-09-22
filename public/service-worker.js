const CACHE_NAME = 'lexmena-cache-v4'; // Incremented version
const baseUrlsToCache = [
  '/',
  '/index.html',
  '/index.tsx', // Assuming index.tsx is served as a static asset or via importmap
  '/lexmena-logo.png', // Your main header logo
  // Note: Tailwind CSS is loaded via CDN, which the browser caches.
  // For robust offline with ESM modules via esm.sh, more advanced caching might be needed.
  // This basic setup caches the main HTML which includes the importmap.
];

// Add icon paths to urlsToCache
const iconSizes = [72, 96, 128, 144, 152, 192, 384, 512];
const iconUrlsToCache = iconSizes.map(size => `/icons/amr-photo-icon-${size}x${size}.png`);

const urlsToCache = [...baseUrlsToCache, ...iconUrlsToCache];

self.addEventListener('install', event => {
  // Perform install steps
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Opened cache');
        // Add all URLs to cache. If any request fails, the service worker installation will fail.
        return cache.addAll(urlsToCache.map(url => new Request(url, { cache: 'reload' })));
      })
      .catch(error => {
        console.error('Failed to cache resources during install:', error);
        // If caching fails, it's better to not install the service worker
        // or to have a fallback strategy. For simplicity, we let it fail here.
      })
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        // Cache hit - return response
        if (response) {
          return response;
        }
        // Not in cache - fetch from network
        return fetch(event.request).then(
          networkResponse => {
            // Check if we received a valid response
            if(!networkResponse || networkResponse.status !== 200 || networkResponse.type !== 'basic'  && networkResponse.type !== 'default') { // "default" for esm.sh
              return networkResponse;
            }

            // IMPORTANT: Clone the response. A response is a stream
            // and because we want the browser to consume the response
            // as well as the cache consuming the response, we need
            // to clone it so we have two streams.
            const responseToCache = networkResponse.clone();

            caches.open(CACHE_NAME)
              .then(cache => {
                cache.put(event.request, responseToCache);
              });

            return networkResponse;
          }
        ).catch(error => {
          console.error('Fetch error:', error);
          // Optionally, return a fallback offline page here if appropriate
          // For a single-page app, usually, the root '/' is cached and handles routing.
        });
      })
  );
});

self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});