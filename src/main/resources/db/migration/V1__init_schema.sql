CREATE TABLE jackpot (
    jackpot_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    initial_pool DECIMAL(19, 2) NOT NULL,
    current_pool DECIMAL(19, 2) NOT NULL,
    contribution_strategy VARCHAR(32) NOT NULL,
    reward_strategy VARCHAR(32) NOT NULL,
    contribution_rate DECIMAL(8, 6),
    min_contribution_rate DECIMAL(8, 6),
    decay_threshold DECIMAL(19, 2),
    decay_slope DECIMAL(8, 6),
    reward_base_probability DECIMAL(8, 6),
    reward_max_probability DECIMAL(8, 6),
    reward_ramp_rate DECIMAL(8, 6),
    reward_cap DECIMAL(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE jackpot_contribution (
    contribution_id UUID PRIMARY KEY,
    bet_id VARCHAR(64) NOT NULL,
    jackpot_id VARCHAR(64) NOT NULL,
    bet_amount DECIMAL(19, 2) NOT NULL,
    contribution_amount DECIMAL(19, 2) NOT NULL,
    post_contribution_pool DECIMAL(19, 2) NOT NULL,
    strategy VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_jackpot_contribution_jackpot FOREIGN KEY (jackpot_id) REFERENCES jackpot (jackpot_id)
);

CREATE TABLE jackpot_reward (
    reward_id UUID PRIMARY KEY,
    bet_id VARCHAR(64) NOT NULL,
    jackpot_id VARCHAR(64) NOT NULL,
    payout_amount DECIMAL(19, 2) NOT NULL,
    probability DECIMAL(8, 6) NOT NULL,
    strategy VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_jackpot_reward_jackpot FOREIGN KEY (jackpot_id) REFERENCES jackpot (jackpot_id)
);

CREATE INDEX idx_jackpot_contribution_bet ON jackpot_contribution (bet_id);
CREATE INDEX idx_jackpot_reward_bet ON jackpot_reward (bet_id);
