DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS races;
DROP TABLE IF EXISTS drivers;
DROP TABLE IF EXISTS sprint_results;
DROP TABLE IF EXISTS driver_standings;

CREATE TABLE races (
                       race_id INT PRIMARY KEY,
                       year INT NOT NULL,
                       round INT,
                       name VARCHAR(100)
);

CREATE TABLE drivers (
                         driver_id INT PRIMARY KEY,
                         driver_ref VARCHAR(50),
                         forename VARCHAR(50),
                         surname VARCHAR(50)
);

CREATE TABLE results (
                         result_id INT PRIMARY KEY,
                         race_id INT NOT NULL,
                         driver_id INT NOT NULL,
                         points DOUBLE PRECISION NOT NULL
);

CREATE TABLE sprint_results (
                                result_id INT PRIMARY KEY,
                                race_id INT NOT NULL,
                                driver_id INT NOT NULL,
                                points DOUBLE PRECISION NOT NULL
);

CREATE TABLE driver_standings (
                                  id BIGSERIAL PRIMARY KEY,
                                  season INT NOT NULL,
                                  position INT NOT NULL,
                                  driver_name VARCHAR(100) NOT NULL,
                                  total_points DOUBLE PRECISION NOT NULL
);

-- optionnel, pour accélérer les requêtes de lecture par saison/position
CREATE INDEX idx_driver_standings_season_position ON driver_standings (season, position);
