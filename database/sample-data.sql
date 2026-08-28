INSERT INTO lender (external_lender_id, display_name, wallet_amount, username, mobile_number, otp_username, otp_password, active, lending_rules)
VALUES ('IAKI7TL1UT6K', 'Abikananda Prusty', 10000.00, 'abikananda', '9090978607', 'abikananda.2012@gmail.com', 'fxfs mjdo rrmd khxg', TRUE, 'REPEATED_LENDERS_LOW_RISK,GOOD_LENDERS')
ON DUPLICATE KEY UPDATE 
    display_name = VALUES(display_name),
    wallet_amount = VALUES(wallet_amount),
    username = VALUES(username),
    mobile_number = VALUES(mobile_number),
    otp_username = VALUES(otp_username),
    otp_password = VALUES(otp_password),
    active = VALUES(active),
    lending_rules = VALUES(lending_rules);
