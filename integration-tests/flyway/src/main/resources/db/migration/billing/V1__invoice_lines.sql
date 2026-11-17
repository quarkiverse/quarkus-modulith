CREATE TABLE invoice_lines (id SERIAL PRIMARY KEY, sku TEXT NOT NULL REFERENCES products (sku));
INSERT INTO migration_log (entry) VALUES ('billing');
