<template>
  <div class="user-profile-component" :class="size">
    <div class="profile-avatar" :style="{ backgroundColor: avatarColor }">
      {{ initials }}
    </div>
    <div class="profile-info">
      <div class="profile-name">{{ name }}</div>
      <div class="profile-role">{{ role }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  user: {
    type: Object,
    required: false,
    default: () => ({
      name: 'Guest',
      role: 'guest',
      id: 0
    })
  },
  size: {
    type: String,
    default: 'medium', // 'small', 'medium', 'large'
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  }
});

// Generate user initials
const initials = computed(() => {
  if (!props.user?.name) return 'U';
  
  const nameParts = props.user.name.split(' ');
  if (nameParts.length === 1) {
    return nameParts[0].charAt(0).toUpperCase();
  }
  
  return (
    nameParts[0].charAt(0).toUpperCase() + 
    nameParts[nameParts.length - 1].charAt(0).toUpperCase()
  );
});

// Format name for display
const name = computed(() => {
  return props.user?.name || 'Guest User';
});

// Format role for display
const role = computed(() => {
  if (!props.user?.role) return 'Guest';
  return props.user.role.charAt(0).toUpperCase() + props.user.role.slice(1).toLowerCase();
});

// Generate consistent avatar color based on user ID
const avatarColor = computed(() => {
  if (!props.user?.id) return '#3498db'; // Default blue
  
  // Generate a consistent color based on user ID
  const colors = [
    '#3498db', // Blue
    '#2ecc71', // Green
    '#e74c3c', // Red
    '#f39c12', // Orange
    '#9b59b6', // Purple
    '#1abc9c', // Turquoise
    '#d35400', // Pumpkin
    '#2c3e50'  // Dark Blue
  ];
  
  const colorIndex = parseInt(props.user.id) % colors.length;
  return colors[colorIndex];
});
</script>

<style scoped>
.user-profile-component {
  display: flex;
  align-items: center;
}

.profile-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: white;
  font-weight: bold;
  margin-right: 12px;
  /* Default medium size */
  width: 40px;
  height: 40px;
  font-size: 16px;
}

.user-profile-component.small .profile-avatar {
  width: 30px;
  height: 30px;
  font-size: 12px;
}

.user-profile-component.large .profile-avatar {
  width: 60px;
  height: 60px;
  font-size: 24px;
}

.profile-info {
  overflow: hidden;
}

.profile-name {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profile-role {
  font-size: 0.8rem;
  color: #718096;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-profile-component.small .profile-name {
  font-size: 0.9rem;
}

.user-profile-component.small .profile-role {
  font-size: 0.7rem;
}

.user-profile-component.large .profile-name {
  font-size: 1.1rem;
}

.user-profile-component.large .profile-role {
  font-size: 0.9rem;
}
</style>