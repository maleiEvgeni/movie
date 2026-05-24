# Movie Search Service

Spring Boot demo skeleton for searching movies and TV series and returning movie metadata enriched with trailer links.

This project is not intended to be a complete production-ready implementation. It is a demonstration of the recommended service structure and implementation approach: API layer, application layer, external provider clients, two-level caching, and basic resilience patterns around outbound calls.

## Purpose

This project is a demo implementation of the `movie-search-service`.

It should be treated as a technical skeleton that shows how this kind of service can be implemented, not as a final enterprise template.

The final production version should align with the target company's existing standards for service discovery, configuration management, observability, deployment, security, logging, and operational ownership.

## Responsibilities

The service demonstrates how to:

- expose movie search and movie details API endpoints;
- retrieve movie and TV metadata from TMDB;
- resolve trailer links from TMDB videos first;
- fall back to YouTube search when TMDB does not provide a usable trailer;
- cache movie search, details, and trailer lookups;
- apply outbound resilience controls for external API calls;
- introduce fallback behavior when an external provider is unavailable.

This service does not contain API Gateway logic.

Authentication, user-level authorization, and inbound rate limiting are expected to be handled by a separate API Gateway. The service can still validate JWT tokens defensively when exposed directly inside the backend network.

## High-level Architecture

```text
Frontend
   |
   | Bearer JWT
   v
API Gateway
   - authentication
   - authorization
   - user-level rate limiting
   - request routing
   - trusted headers
   |
   v
Movie Search Service
   - movie search API
   - movie details API
   - trailer resolution
   - local cache
   - Redis cache
   - outbound rate limiting
   - circuit breaker
   - retry
   - bulkhead
   - fallback handling
   |
   +--> TMDB API
   |
   +--> YouTube Data API
```

## Runtime Flow

### Movie Search

```http
GET /api/v1/movies/search?query=matrix&page=1&language=en-US
```

Flow:

```text
1. Frontend sends an authenticated request.
2. API Gateway validates the user and forwards the request.
3. Movie Search Service checks L1 local cache.
4. If not found, it checks L2 Redis cache.
5. If not found, it calls TMDB /search/multi.
6. Search results are mapped to internal API DTOs.
7. Trailer enrichment is applied only to a limited number of top results.
8. Response is returned to frontend.
```

Trailer enrichment is intentionally limited for search results.

Reason:

```text
One frontend search request can return many movies.
Resolving trailers for every result could multiply a single request into many external provider calls.
That would increase latency and could quickly exhaust YouTube/TMDB quotas.
```

### Movie Details

```http
GET /api/v1/movies/{tmdbId}
GET /api/v1/movies/tv/{tmdbId}
```

Flow:

```text
1. Service checks details cache.
2. If not cached, it calls TMDB movie or TV details endpoint.
3. Service resolves the best available trailer.
4. Full movie details are returned.
```

For the details endpoint, trailer resolution is always attempted because it is a focused user action.

## Provider Fallback Strategy

The current skeleton uses TMDB as the primary movie metadata provider.

In a production implementation, a fallback strategy can be added for cases where TMDB is unavailable, rate-limited, or returns temporary errors.

Possible fallback options:

```text
1. Return stale cached data if available.
2. Use another movie metadata provider as a secondary datasource.
3. Return a degraded response with partial data and a clear warning.
4. Fail fast with a controlled 503 response if no safe fallback exists.
```

A fallback provider should not be added blindly. It needs explicit mapping rules, data-quality checks, provider-specific rate limits, and clear conflict-resolution logic when providers return different metadata.

## Trailer Resolution Strategy

Trailer lookup priority:

```text
1. TMDB videos endpoint
2. YouTube Search API fallback
3. No trailer returned
```

TMDB videos are preferred because they are already linked to the movie or TV entity. YouTube search is less reliable because it can return reviews, fan videos, reactions, or unrelated content.

## Cache Design

The service uses two-level caching:

```text
L1 cache: Caffeine
L2 cache: Redis
```

### Why two levels?

```text
Caffeine:
  - very fast local in-memory cache
  - reduces repeated Redis calls
  - short TTL

Redis:
  - shared between service instances
  - survives service restarts
  - longer TTL
```

### Cache Types

```text
movie-search
movie-details
movie-trailer
```

Example cache keys:

```text
movie-search:v1:{language}:{page}:{normalizedQuery}
movie-details:v1:{type}:{language}:{tmdbId}
movie-trailer:best:v1:{type}:{language}:{tmdbId}
```

## External Provider Protection

External calls are protected with Resilience4j primitives:

```text
CircuitBreaker
RateLimiter
Retry
Bulkhead
```

### Circuit Breaker

Prevents repeated calls to failing providers.

Example:

```text
If TMDB starts returning errors or timing out,
the circuit breaker opens and the service stops calling TMDB temporarily.
```

### Rate Limiter

Controls outbound request rate to external APIs.

This is separate from API Gateway rate limiting.

```text
Gateway rate limit:
  protects our public API from users.

Service outbound rate limit:
  protects external provider quotas and our internal thread pool.
```

### Retry

Used only for transient failures.

Retry should not be used aggressively for:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
429 Too Many Requests
```

### Bulkhead

Limits concurrent calls to each external provider.

This prevents one slow provider from consuming all service resources.

## Agent Documentation and Skills

For real development work, the repository should also include documentation that helps engineering agents and automation tools operate safely in the codebase.

Recommended additions:

```text
README files:
  - service overview
  - local setup
  - architecture decisions
  - operational assumptions
  - known limitations

Agent skills / agent instructions:
  - coding conventions
  - package structure rules
  - testing expectations
  - resilience and fallback rules
  - cache usage rules
  - security boundaries
  - rules for adding new external providers
```

These instructions should prevent agents from making unsafe architectural changes, bypassing the API Gateway boundary, removing resilience wrappers, leaking provider secrets, or coupling frontend contracts directly to external provider DTOs.

## API Contract Documentation

This demo exposes controller endpoints directly, but the API contract should be documented explicitly.

In a production implementation, the contract should be described with Swagger/OpenAPI or another API documentation tool accepted by the organization.

The contract should define:

```text
- endpoint paths
- request parameters
- response DTOs
- error responses
- authentication requirements
- pagination rules
- rate-limit behavior
- degraded-response behavior
```

Frontend teams should rely on the documented contract, not on accidental details of external provider responses.

## API Endpoints

### Search Movies and TV Series

```http
GET /api/v1/movies/search?query=matrix&page=1&language=en-US
Authorization: Bearer <jwt>
```

Response example:

```json
{
  "query": "matrix",
  "page": 1,
  "totalPages": 10,
  "totalResults": 200,
  "results": [
    {
      "id": "movie:603",
      "tmdbId": 603,
      "title": "The Matrix",
      "overview": "A computer hacker learns...",
      "releaseDate": "1999-03-31",
      "posterUrl": "https://image.tmdb.org/t/p/w500/...",
      "backdropUrl": "https://image.tmdb.org/t/p/w780/...",
      "rating": 8.2,
      "type": "MOVIE",
      "trailer": {
        "provider": "youtube",
        "url": "https://www.youtube.com/watch?v=...",
        "source": "tmdb-videos",
        "official": true
      }
    }
  ]
}
```

### Movie Details

```http
GET /api/v1/movies/{tmdbId}
Authorization: Bearer <jwt>
```

### TV Details

```http
GET /api/v1/movies/tv/{tmdbId}
Authorization: Bearer <jwt>
```

## Enterprise Landscape Considerations

This demo uses simple local configuration to keep the implementation readable.

In a real corporate environment, the following concerns should follow the standards already accepted in the company landscape:

```text
Service discovery:
  - Kubernetes service discovery, Consul, Eureka, cloud-native discovery, or another internal standard.

Configuration management:
  - environment variables, Spring Cloud Config, Vault, Kubernetes ConfigMaps/Secrets, or the company's existing configuration platform.

Secrets management:
  - API keys and tokens should not be stored in source control.
  - Provider credentials should come from the approved secret-management solution.

Monitoring and observability:
  - metrics, logs, traces, dashboards, and alerts should integrate with the corporate monitoring stack.

Deployment:
  - packaging, containerization, health checks, readiness probes, and rollout strategy should match the target platform.

Security:
  - authentication, authorization, trusted headers, network access, and service-to-service communication should follow corporate security standards.
```

The code in this repository intentionally keeps these parts simple because the exact implementation depends on the existing enterprise landscape.

## Configuration

Main configuration is stored under the `movie` prefix.

Example:

```yaml
movie:
  tmdb:
    base-url: https://api.themoviedb.org/3
    image-base-url: https://image.tmdb.org/t/p
    api-key: ${TMDB_API_KEY}
    connect-timeout: 2s
    read-timeout: 5s

  youtube:
    base-url: https://www.googleapis.com/youtube/v3
    api-key: ${YOUTUBE_API_KEY:}
    connect-timeout: 2s
    read-timeout: 5s

  cache:
    search-l1-ttl: 2m
    search-l2-ttl: 30m
    details-l1-ttl: 10m
    details-l2-ttl: 12h
    trailer-l1-ttl: 15m
    trailer-l2-ttl: 3d
```

## Design Decisions

That is too expensive for latency and external API quotas.

The service enriches only a limited number of top search results. Full trailer resolution is done on the details endpoint.

### Why keep API Gateway separate?

Gateway concerns are different from movie search concerns.

API Gateway should handle:

```text
authentication
authorization
inbound rate limiting
routing
trusted headers
```

Movie Search Service should handle:

```text
movie data aggregation
external API integration
cache
outbound resilience
provider fallback
```

Mixing both in one service would make the design harder to reason about and harder to scale independently.

## Testing and Quality Gates

The demo skeleton should be extended with automated tests and quality controls before being treated as production-ready.

Recommended test layers:

```text
Unit tests:
  - mapping logic
  - trailer selection logic
  - cache key generation
  - fallback decision logic

Integration tests:
  - TMDB client with mocked provider responses
  - YouTube client with mocked provider responses
  - Redis cache integration
  - controller contract tests

Resilience tests:
  - circuit breaker opens on repeated provider failures
  - rate limiter rejects excessive outbound calls
  - bulkhead rejects calls when provider concurrency is exhausted
  - fallback returns degraded response instead of failing the whole search
```

Quality gates should be defined explicitly by the team.

Examples:

```text
- minimum line and branch coverage thresholds
- required tests for provider clients and fallback paths
- static analysis checks
- dependency vulnerability checks
- mutation testing for critical business logic, for example with PIT
```

Mutation testing is especially useful for trailer selection, fallback decisions, and cache behavior because these areas can appear covered by tests while still having weak assertions.

## Current Limitations

```text
- This is a demo skeleton, not a complete production implementation.
- No negative cache for missing trailers yet.
- No fallback metadata provider for TMDB unavailability yet.
- No Vimeo fallback implemented yet.
- No persistent storage.
- No async trailer enrichment.
- No provider quota dashboard.
- No Swagger/OpenAPI contract added yet.
- No dedicated agent instructions or skills added yet.
- No automated test suite added yet.
- No explicit quality gates for coverage or mutation testing added yet.
- No corporate service discovery integration added yet.
- No corporate configuration/secrets management integration added yet.
- No corporate observability integration added yet.
```

## Recommended Improvements

```text
1. Add Swagger/OpenAPI or another approved API contract documentation tool.
2. Add stale-cache fallback for TMDB unavailability.
3. Consider adding a secondary movie metadata provider if the business requires higher availability.
4. Add negative cache for missing trailers.
5. Add integration tests with WireMock or an equivalent tool.
6. Add provider-specific error mapping.
7. Add metrics for cache hit ratio.
8. Add metrics for TMDB and YouTube calls.
9. Integrate service discovery according to the corporate platform standard.
10. Integrate configuration and secrets management according to the corporate standard.
11. Integrate logs, metrics, tracing, dashboards, and alerts with the corporate observability stack.
12. Add repository-level README and agent skills/instructions for safe automated development.
13. Add unit, integration, contract, and resilience tests.
14. Add quality gates for test coverage and mutation testing, for example with PIT.
15. Add API Gateway as a separate Gradle module or separate service.
```