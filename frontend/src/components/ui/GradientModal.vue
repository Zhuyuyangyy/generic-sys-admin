<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleOverlayClick">
        <div
          class="modal-container"
          :class="[`size-${size}`, `variant-${variant}`, { 'has-particles': particles }]"
          role="dialog"
          :aria-labelledby="title ? 'modal-title' : undefined"
          aria-modal="true"
        >
          <!-- Background Effects -->
          <div class="modal-bg-gradient" aria-hidden="true"></div>
          <div class="modal-glow-tl" aria-hidden="true"></div>
          <div class="modal-glow-br" aria-hidden="true"></div>
          
          <!-- Particles -->
          <div v-if="particles" class="modal-particles" aria-hidden="true">
            <span v-for="i in 6" :key="i" class="particle"></span>
          </div>

          <!-- Header -->
          <div class="modal-header">
            <h2 id="modal-title" class="modal-title">
              <slot name="title">{{ title }}</slot>
            </h2>
            <button class="modal-close" @click="close" aria-label="关闭">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <!-- Body -->
          <div class="modal-body">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '',
  },
  size: {
    type: String,
    default: 'md',
  },
  variant: {
    type: String,
    default: 'default',
  },
  closeOnOverlay: {
    type: Boolean,
    default: true,
  },
  particles: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue', 'close'])

function close() {
  emit('update:modelValue', false)
  emit('close')
}

function handleOverlayClick() {
  if (props.closeOnOverlay) close()
}
</script>

<style scoped>
/* ================================================
   Gradient Modal — Enhanced Galaxy Style
   Inspired by Uiverse.io Galaxy library
   Enhanced with glow effects and particles
   ================================================ */

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(6px);
  padding: 20px;
}

.modal-container {
  position: relative;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18),
              0 4px 20px rgba(102, 126, 234, 0.12);
  overflow: hidden;
  font-family: 'Segoe UI', system-ui, sans-serif;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

/* Sizes */
.size-sm { width: 380px; }
.size-md { width: 520px; }
.size-lg { width: 720px; }

/* === Background Gradient === */
.modal-bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.03) 0%, rgba(118, 75, 162, 0.03) 100%);
  pointer-events: none;
}

/* === Glow Effects === */
.modal-glow-tl {
  position: absolute;
  top: -80px;
  left: -80px;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  animation: pulse-glow 3s ease-in-out infinite;
}

.modal-glow-br {
  position: absolute;
  bottom: -100px;
  right: -100px;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(118, 75, 162, 0.12) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  animation: pulse-glow 3s ease-in-out infinite reverse;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}

/* === Particles === */
.modal-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  opacity: 0;
  animation: float-particle 4s ease-in-out infinite;
}

.particle:nth-child(1) { left: 10%; animation-delay: 0s; }
.particle:nth-child(2) { left: 25%; animation-delay: 0.5s; }
.particle:nth-child(3) { left: 40%; animation-delay: 1s; }
.particle:nth-child(4) { left: 55%; animation-delay: 1.5s; }
.particle:nth-child(5) { left: 70%; animation-delay: 2s; }
.particle:nth-child(6) { left: 85%; animation-delay: 2.5s; }

@keyframes float-particle {
  0% {
    bottom: -10px;
    opacity: 0;
    transform: scale(0);
  }
  20% {
    opacity: 0.8;
    transform: scale(1);
  }
  80% {
    opacity: 0.3;
  }
  100% {
    bottom: 100%;
    opacity: 0;
    transform: scale(0.5);
  }
}

/* === Header === */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px 20px;
  border-bottom: 1px solid #f1f5f9;
  position: relative;
  z-index: 1;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  letter-spacing: -0.01em;
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #f1f5f9;
  border-radius: 10px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  flex-shrink: 0;
}

.modal-close:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  transform: rotate(90deg) scale(1.05);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35);
}

/* === Body === */
.modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
  flex: 1;
  position: relative;
  z-index: 1;
}

/* === Footer === */
.modal-footer {
  padding: 18px 28px 24px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  position: relative;
  z-index: 1;
}

/* === Variant: Glass === */
.variant-glass {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.12),
              inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.variant-glass .modal-header {
  border-bottom-color: rgba(102, 126, 234, 0.1);
}

.variant-glass .modal-footer {
  border-top-color: rgba(102, 126, 234, 0.1);
}

/* === Variant: Cyber === */
.variant-cyber {
  background: linear-gradient(135deg, #1e1e2e 0%, #2d2d44 100%);
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.variant-cyber .modal-header {
  border-bottom-color: rgba(102, 126, 234, 0.2);
}

.variant-cyber .modal-title {
  color: #e2e8f0;
}

.variant-cyber .modal-body {
  color: #94a3b8;
}

.variant-cyber .modal-footer {
  border-top-color: rgba(102, 126, 234, 0.2);
}

.variant-cyber .modal-close {
  background: rgba(102, 126, 234, 0.15);
  color: #94a3b8;
}

.variant-cyber .modal-close:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

/* === Transitions === */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.92) translateY(20px);
  opacity: 0;
}

.modal-enter-to .modal-container,
.modal-leave-from .modal-container {
  transform: scale(1) translateY(0);
  opacity: 1;
}
</style>
