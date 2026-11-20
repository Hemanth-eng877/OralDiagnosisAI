import logging
logger = logging.getLogger('clinical_audit')

def log_clinical_access(user_id, action, target_id):
    logger.info(f'AUDIT: User {user_id} performed {action} on target {target_id}')
