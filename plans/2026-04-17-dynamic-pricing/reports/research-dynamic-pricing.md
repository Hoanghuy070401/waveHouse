# Research Report: Dynamic Pricing & Price History

## Objective
Research best practices for tracking product price changes over time in a NoSQL database (Firebase Realtime Database), applying it to the FreshStock pricing model.

## Context
- The app uses **Firebase Realtime Database**.
- The `Product` model needs to track `costPrice` and `salePrice`.
- Prices fluctuate frequently (Dynamic Pricing).
- Existing order prices must be frozen (already handled by creating stateless OrderItems).

## Findings
Storing an ever-growing list of changes inside a single NoSQL document leads to performance degradation and size limit issues.

### Approach 1: Flat History Nodes (Recommended)
Store the current price directly on the product, and append a log entry to a separate path.
**Data Structure:**
- `/products/{productId}`: `{ name, currentPrice, ... }`
- `/product_price_history/{productId}/{pushId}`: `{ costPrice, salePrice, timestamp, updatedBy }`

**Pros:**
- Product list reads remain extremely fast and lightweight.
- Highly scalable; history can grow indefinitely.
**Cons:**
- Requires fan-out/multi-path updates to keep data consistent.

### Approach 2: Embedded Array / Map
Store history directly inside the Product object.
**Data Structure:**
- `/products/{productId}`: `{ name, currentPrice, priceHistory: { pushId: { ... } } }`

**Pros:**
- Atomic updates are trivial.
- A single read retrieves both product info and its full history.
**Cons:**
- Fetching a list of 100 products also fetches 100x their entire price histories, wasting bandwidth.
- May hit document size limits quickly if prices change daily.

## Conclusion
Approach 1 is mandatory for a scalability-focused "FreshStock" application where prices might change daily based on market conditions.
