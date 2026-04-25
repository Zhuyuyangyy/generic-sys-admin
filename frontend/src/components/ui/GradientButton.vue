<template>
  <button
    class="gradient-button"
    :class="[
      `variant-${variant}`,
      `size-${size}`,
      {
        loading: loading,
        disabled: disabled,
        'pulse-effect': pulse,
        'neon-glow': neon
      }
    ]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span class="btn-content">
      <span v-if="loading" class="btn-spinner"></span>
      <slot />
    </span>
    <span class="btn-glow" aria-hidden="true"></span>
    <span class="btn-shine" aria-hidden="true"></span>
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
  },
  size: {
    type: String,
    default: 'md',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  pulse: {
    type: Boolean,
    default: false,
  },
  neon: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['click'])
</script>

<style scoped>
/* ================================================
   Gradient Button — Enhanced Galaxy Style
   Inspired by Uiverse.io Galaxy library
   Enhanced with neon glow, pulse, and micro-interactions
   ================================================ */

/* === Base === */
.gradient-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  font-family: inherit;
  letter-spacing: 0.05em;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
}

.gradient-button:focus {
  outline: none;
}

.gradient-button:focus-visible {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.4);
}

/* === Sizes === */
.size-sm {
  padding: 8px 20px;
  border-radius: 50px;
  font-size: 13px;
}

.size-md {
  padding: 12px 28px;
  border-radius: 50px;
  font-size: 15px;
}

.size-lg {
  padding: 16px 40px;
  border-radius: 50px;
  font-size: 17px;
}

/* === Variants === */

/* Primary: Blue-Purple Gradient */
.variant-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35);
}

.variant-primary:hover:not(.disabled):not(.loading) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}

.variant-primary:active:not(.disabled):not(.loading) {
  transform: translateY(0) scale(0.97);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

/* Secondary: Cyan-Blue Gradient */
.variant-secondary {
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(6, 182, 212, 0.3);
}

.variant-secondary:hover:not(.disabled):not(.loading) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(6, 182, 212, 0.45);
}

.variant-secondary:active:not(.disabled):not(.loading) {
  transform: translateY(0) scale(0.97);
}

/* Outline: Transparent with Gradient Border */
.variant-outline {
  background: transparent;
  color: #667eea;
  border: 2px solid transparent;
  background-image: linear-gradient(white, white),
                    linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-origin: border-box;
  background-clip: padding-box, border-box;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.15);
}

.variant-outline:hover:not(.disabled):not(.loading) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
  background-image: linear-gradient(#f8fafc, #f8fafc),
                    linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.variant-outline:active:not(.disabled):not(.loading) {
  transform: translateY(0) scale(0.97);
}

/* Success: Green Gradient */
.variant-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

.variant-success:hover:not(.disabled):not(.loading) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.45);
}

.variant-success:active:not(.disabled):not(.loading) {
  transform: translateY(0) scale(0.97);
}

/* Danger: Red-Orange Gradient */
.variant-danger {
  background: linear-gradient(135deg, #ef4444 0%, #f97316 100%);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
}

.variant-danger:hover:not(.disabled):not(.loading) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(239, 68, 68, 0.45);
}

.variant-danger:active:not(.disabled):not(.loading) {
  transform: translateY(0) scale(0.97);
}

/* === Glow Effect === */
.btn-glow {
  position: absolute;
  top: 0;
  right: 0;
  width: 45px;
  height: 100%;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50px 0 0 50px;
  transition: all 0.8s cubic-bezier(0.25, 0.8, 0.25, 1);
  pointer-events: none;
}

.variant-primary:hover .btn-glow,
.variant-secondary:hover .btn-glow,
.variant-success:hover .btn-glow,
.variant-danger:hover .btn-glow {
  width: 100%;
  border-radius: 50px;
  background: rgba(255, 255, 255, 0.12);
}

/* === Shine Effect === */
.btn-shine {
  position: absolute;
  top: 0;
  left: -75%;
  width: 50%;
  height: 100%;
  background: linear-gradient(
    to right,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  transform: skewX(-25deg);
  transition: left 0.6s ease;
  pointer-events: none;
}

.gradient-button:hover .btn-shine {
  left: 125%;
}

/* === Neon Glow Effect === */
.neon-glow.variant-primary {
  animation: neon-pulse-primary 2s infinite;
}

.neon-glow.variant-secondary {
  animation: neon-pulse-secondary 2s infinite;
}

@keyframes neon-pulse-primary {
  0%, 100% {
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35),
                0 0 5px rgba(102, 126, 234, 0.2);
  }
  50% {
    box-shadow: 0 4px 25px rgba(102, 126, 234, 0.6),
                0 0 20px rgba(102, 126, 234, 0.4),
                0 0 40px rgba(102, 126, 234, 0.2);
  }
}

@keyframes neon-pulse-secondary {
  0%, 100% {
    box-shadow: 0 4px 15px rgba(6, 182, 212, 0.3),
                0 0 5px rgba(6, 182, 212, 0.2);
  }
  50% {
    box-shadow: 0 4px 25px rgba(6, 182, 212, 0.5),
                0 0 20px rgba(6, 182, 212, 0.35),
                0 0 40px rgba(6, 182, 212, 0.15);
  }
}

/* === Pulse Effect === */
.pulse-effect::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: inherit;
  animation: pulse-ring 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  z-index: -1;
}

@keyframes pulse-ring {
  0%, 100% {
    opacity: 0;
    transform: scale(1);
  }
  50% {
    opacity: 0.3;
    transform: scale(1.05);
  }
}

/* === Content === */
.btn-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* === Loading Spinner === */
.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* === Loading State === */
.loading {
  pointer-events: none;
  opacity: 0.8;
}

/* === Disabled === */
.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}
</style>
