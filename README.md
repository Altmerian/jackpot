# Jackpot Service

Backend service for managing jackpot contributions and rewards.

## Prerequisites

- Java 25+
- Docker & Docker Compose

## How to Run

1. **Start Kafka infrastructure:**
   ```bash
   docker compose up --build -d
   ```

2. **Build and run the service:**
   ```bash
   ./gradlew clean build
   ./gradlew bootRun
   ```

   The service will start on `http://localhost:8080`

3. **Run tests:**
   ```bash
   ./gradlew test
   ```

## Basic Verification

The service provides two main endpoints matching the assignment requirements:

### 1. Submit a Bet (Publishes to Kafka)

Submit a bet which will be published to Kafka topic `jackpot-bets` and processed asynchronously:

```bash
curl -X POST http://localhost:8080/api/bets \
  -H "Content-Type: application/json" \
  -d '{
    "betId": "bet-001",
    "userId": "user-123",
    "jackpotId": "fixed-warmup",
    "betAmount": 50.0
  }'
```

**Expected Response:** `202 Accepted`
```json
{
  "betId": "bet-001"
}
```

### 2. Evaluate Jackpot Reward

Evaluate if a bet wins a jackpot reward (requires the bet to have been contributed first):

```bash
# Wait a moment for Kafka consumer to process the bet contribution
sleep 2

# Evaluate the bet for reward
curl "http://localhost:8080/api/evaluations?betId=bet-001&jackpotId=fixed-warmup"
```

**Expected Response:** `200 OK`
```json
{
  "win": true,
  "payoutAmount": 505.00,
  "currentJackpotPool": 500.00,
  "probability": 0.050000,
  "strategy": "FIXED",
  "betId": "bet-001",
  "jackpotId": "fixed-warmup"
}
```

**Note:** Evaluation uses deterministic RNG seeded by `betId + jackpotId`, so the same bet always produces the same outcome.


## Implemented Features

✅ REST API endpoint to publish bets to Kafka
✅ Kafka consumer listening to `jackpot-bets` topic
✅ Contribution processing with two strategies (Fixed, Variable Decay)
✅ Reward evaluation with two strategies (Fixed, Variable Ramp)
✅ REST API endpoint to evaluate jackpot rewards
✅ Pool reset when jackpot is won
✅ H2 in-memory database for persistence
✅ Locking to prevent concurrent evaluation issues
✅ Deterministic outcomes for testing

## Configuration

### Jackpot Profiles

Two jackpot profiles are pre-configured in `application.yml`:

- **fixed-warmup**: 10% fixed contribution, 5% fixed reward probability
- **decaying-marathon**: Variable contribution (starts 12%, decays to 4%), ramping reward probability

### Contribution Strategies

Contribution strategies determine how much of each bet goes into the jackpot pool:

#### Fixed Rate Strategy
- **Behavior**: Constant percentage of bet amount contributes to the pool
- **Formula**: `contribution = betAmount × fixedRate`
- **Example**: With 10% fixed rate, a $100 bet contributes $10
- **Use Case**: Simple, predictable jackpot growth

#### Variable Decay Strategy
- **Behavior**: Contribution percentage decreases as the jackpot pool grows
- **Formula**: `effectiveRate = max(minRate, initialRate - (decaySlope × (currentPool / decayThreshold)))`
- **Example**: Starts at 12% contribution, gradually decays to minimum 4% as pool approaches threshold
- **Use Case**: Accelerates early pool growth, slows as pool becomes large

### Reward Strategies

Reward strategies determine the probability of winning the jackpot:

#### Fixed Probability Strategy
- **Behavior**: Constant win chance regardless of pool size
- **Formula**: `probability = rewardBaseProbability`
- **Example**: 5% chance on every evaluation
- **Use Case**: Predictable, fair odds for all players

#### Variable Ramp Strategy
- **Behavior**: Win probability increases as jackpot pool approaches its cap
- **Formula**: `probability = min(maxProbability, baseProbability + rampRate × (currentPool / rewardCap))`
- **Special Rule**: When `currentPool >= rewardCap`, probability becomes 100%
- **Example**: Starts at 1% probability, increases linearly, reaches 100% when pool is full
- **Use Case**: Guarantees eventual payout, creates urgency as pool grows

### Strategy Extensibility

The system uses the **Strategy Pattern** to support multiple configurations:
- New contribution strategies can be added by implementing `ContributionStrategy` interface
- New reward strategies can be added by implementing `RewardStrategy` interface
- Strategies are registered in `StrategyRegistry` and selected based on jackpot configuration
- No code changes needed to add new profiles for existing strategies types, just configuration in the `application.yml`

## Health Check

```bash
curl http://localhost:8080/actuator/health
```
