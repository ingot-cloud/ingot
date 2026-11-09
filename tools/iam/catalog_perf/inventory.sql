-- Run against the IAM database through a read-only connection.
START TRANSACTION READ ONLY;

SELECT
    DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%dT%H:%i:%s.%fZ') AS observed_at,
    CAST(app.id AS CHAR) AS application_id,
    app.code AS application_code,
    app.domain,
    app.enabled AS application_enabled,
    COUNT(act.id) AS platform_actions,
    COUNT(CASE WHEN act.enabled = TRUE AND res.enabled = TRUE THEN act.id END) AS tenant_actions
FROM iam_application app
LEFT JOIN iam_action act ON act.application_id = app.id
LEFT JOIN iam_resource res ON res.application_id = act.application_id AND res.id = act.resource_id
GROUP BY app.id, app.code, app.domain, app.enabled
ORDER BY app.id;

COMMIT;
