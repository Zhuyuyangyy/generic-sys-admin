<template>
  <label class="toggle-wrapper" :class="{ checked: modelValue, disabled }">
    <input
      type="checkbox"
      class="toggle-input"
      :checked="modelValue"
      :disabled="disabled"
      @change="$emit('update:modelValue', $event.target.checked)"
    />
    <span class="toggle-track" aria-hidden="true">
      <span class="toggle-thumb">
        <span class="thumb-inner"></span>
      </span>
      <span class="toggle-particles" aria-hidden="true">
        <span v-for="i in 3" :key="i" class="particle"></span>
      </span>
    </span>
    <span v-if="label" class="toggle-label">{{ label }}</span>
  </label>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  label: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
/* ================================================
   Gradient Toggle Switch — Enhanced Galaxy Style
   Inspired by Uiverse.io Galaxy library
   Enhanced with particles and glow effects
   ================================================ */

.toggle-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  font-family: 'Segoe UI', system-ui, sans-serif;
  user-select: none;
}

.toggle-wrapper.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.toggle-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-track {
  position: relative;
  width: 52px;
  height: 28px;
  background: #e2e8f0;
  border-radius: 50px;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  flex-shrink: 0;
}

.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-inner {
  width: 8px;
  height: 8px;
  background: #e2e8f0;
  border-radius: 50%;
  transition: all 0.3s ease;
}

/* Checked state */
.checked .toggle-track {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4),
              0 0 20px rgba(102, 126, 234, 0.3);
}

.checked .toggle-thumb {
  transform: translateX(24px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.checked .thumb-inner {
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
}

/* Particles */
.toggle-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: #ffffff;
  border-radius: 50%;
  opacity: 0;
  top: 50%;
  transform: translateY(-50%);
}

.checked .particle {
  animation: sparkle 1.5s ease-in-out infinite;
}

.particle:nth-child(1) {
  left: 6px;
  animation-delay: 0s;
}

.particle:nth-child(2) {
  left: 12px;
  animation-delay: 0.2s;
}

.particle:nth-child(3) {
  left: 18px;
  animation-delay: 0.4s;
}

@keyframes sparkle {
  0%, 100% {
    opacity: 0;
    transform: translateY(-50%) scale(0);
  }
  25% {
    opacity: 1;
    transform: translateY(-50%) scale(1);
  }
  75% {
    opacity: 0.5;
    transform: translateY(-50%) translateX(8px) scale(0.5);
  }
  100% {
    opacity: 0;
    transform: translateY(-50%) translateX(12px) scale(0);
  }
}

/* Hover */
.toggle-wrapper:hover:not(.disabled) .toggle-track {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.checked:hover:not(.disabled) .toggle-track {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.5),
              0 0 25px rgba(102, 126, 234, 0.35);
}

/* Focus */
.toggle-input:focus-visible + .toggle-track {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.3);
}

.toggle-label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  transition: color 0.3s ease;
}

.checked .toggle-label {
  color: #1e293b;
}
</style>
