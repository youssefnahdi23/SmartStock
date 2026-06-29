-- V2: Add supplier_risk_assessment table

CREATE TABLE supplier_risk_assessment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    assessment_date DATE NOT NULL,
    financial_health_score DECIMAL(3, 2),
    delivery_reliability_score DECIMAL(3, 2),
    quality_consistency_score DECIMAL(3, 2),
    communication_responsiveness_score DECIMAL(3, 2),
    compliance_score DECIMAL(3, 2),
    overall_risk_score DECIMAL(3, 2),
    risk_level VARCHAR(50),
    key_risks TEXT,
    mitigation_actions TEXT,
    next_assessment_date DATE,
    assessed_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_assessment UNIQUE (supplier_id, assessment_date),
    CONSTRAINT valid_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_risk_assessment_supplier ON supplier_risk_assessment(supplier_id);
CREATE INDEX idx_risk_assessment_date ON supplier_risk_assessment(assessment_date);
CREATE INDEX idx_risk_assessment_level ON supplier_risk_assessment(risk_level);
