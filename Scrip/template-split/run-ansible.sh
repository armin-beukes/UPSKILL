#!/usr/bin/env bash
# Reformatted for readability: variables split, quoted paths, and comments.

# Path to ansible config and roles (contains spaces so keep quoted)
ANSIBLE_CONFIG="/mnt/d/DEVOPS/Trafman T400/trafman-devops/ansible/docker-trafman/ansible.cfg"
ANSIBLE_ROLES_PATH="/mnt/d/DEVOPS/Trafman T400/trafman-devops/ansible/roles"

# Playbook and inventory
PLAYBOOK="/mnt/d/DEVOPS/Trafman T400/trafman-devops/ansible/docker-trafman/../docker-playbooks/setupDevOpsUserPlaybook.yaml"
INVENTORY="/mnt/d/DEVOPS/Trafman T400/trafman-devops/ansible/docker-trafman/trafman_inv.yaml"

# Execution options
LIMIT="abk-dev"
EXTRA_VARS="ansible_base_dir=/mnt/d/DEVOPS/Trafman T400/trafman-devops/ansible/docker-trafman"

# Run the command (preserve environment variables for this single invocation)
ANSIBLE_CONFIG="$ANSIBLE_CONFIG" \
ANSIBLE_ROLES_PATH="$ANSIBLE_ROLES_PATH" \
	uv run ansible-playbook "$PLAYBOOK" \
		-i "$INVENTORY" \
		--limit "$LIMIT" \
		--ask-vault-pass \
		--extra-vars "$EXTRA_VARS"

# End