-- Add missing columns to customer_preferences (DB spec §2.4)
ALTER TABLE customer_preferences
    ADD COLUMN IF NOT EXISTS receive_phone_communications  BOOLEAN     DEFAULT true,
    ADD COLUMN IF NOT EXISTS receive_product_updates       BOOLEAN     DEFAULT true,
    ADD COLUMN IF NOT EXISTS receive_shipping_updates      BOOLEAN     DEFAULT true,
    ADD COLUMN IF NOT EXISTS do_not_contact_until          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS communication_frequency       VARCHAR(50) DEFAULT 'AS_NEEDED';
