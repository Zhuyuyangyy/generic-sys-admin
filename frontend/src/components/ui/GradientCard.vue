<template>
  <div
    class="gradient-card"
    :class="[
      `variant-${variant}`,
      {
        'hover-lift': hoverLift,
        'hover-glow': hoverGlow,
        '3d-flip': flip,
        'interactive': interactive
      }
    ]"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <div v-if="title || $slots.header" class="card-header">
      <slot name="header">
        <span class="card-title">{{ title }}</span>
        <span v-if="badge" class="card-badge">{{ badge }}</span>
      </slot>
    </div>

    <div class="card-body">
      <slot />
    </div>

    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </div>

    <div class="card-shine" aria-hidden="true"></div>
    <div class="card-border-glow" aria-hidden="true"></div>
    <div class="card-floating-particles" aria-hidden="true">
      <span v-for="i in 3" :key="i" class="particle"></span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  title: {
    type: String,
    default: '',
  },
  badge: {
    type: String,
    default: '',
  },
  variant: {
    type: String,
    default: 'default',
  },
  hoverLift: {
    type: Boolean,
    default: true,
  },
  hoverGlow: {
    type: Boolean,
    default: true,
  },
  flip: {
    type: Boolean,
    default: false,
  },
  interactive: {
    type: Boolean,
    default: true,
  },
})

const isHovered = ref(false)
</script>

<style scoped>
/* ================================================
   Gradient Card — Enhanced Galaxy Style
   Inspired by Uiverse.io Galaxy library
   Enhanced with glow effects, particles, and 3D transforms
   ================================================ */

.gradient-card {
  position: relative;
  background: #ffffff;
  border-radius: 20px;
  padding: 24px;
  overflow: hidden;
  transition: all 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
  font-family: 'Segoe UI', system-ui, sans-serif;
}

/* === Variants === */

.variant-default {
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.variant-default:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.12),
              0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: rgba(102, 126, 234, 0.25);
}

/* Elevated — stronger shadow */
.variant-elevated {
  border: 1px solid rgba(102, 126, 234, 0.15);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.1),
              0 1px 6px rgba(0, 0, 0, 0.04);
}

.variant-elevated:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(102, 126, 234, 0.18),
              0 6px 16px rgba(0, 0, 0, 0.06);
}

/* Glass — frosted glass effect */
.variant-glass {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08),
              inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.variant-glass:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-3px);
  box-shadow: 0 16px 48px rgba(102, 126, 234, 0.12),
              inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

/* Neon variant — glowing border */
.variant-neon {
  border: 1px solid rgba(102, 126, 234, 0.3);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.1),
              inset 0 0 20px rgba(102, 126, 234, 0.05);
}

.variant-neon:hover {
  border-color: rgba(102, 126, 234, 0.6);
  box-shadow: 0 8px 40px rgba(102, 126, 234, 0.25),
              0 0 30px rgba(102, 126, 234, 0.15),
              inset 0 0 30px rgba(102, 126, 234, 0.08);
  transform: translateY(-4px);
}

/* Cyber — dark theme card */
.variant-cyber {
  background: linear-gradient(135deg, #1e1e2e 0%, #2d2d44 100%);
  border: 1px solid rgba(102, 126, 234, 0.3);
  color: #e2e8f0;
}

.variant-cyber:hover {
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 8px 40px rgba(102, 126, 234, 0.2),
              inset 0 0 30px rgba(102, 126, 234, 0.08);
  transform: translateY(-4px);
}

/* === Hover Lift === */
.hover-lift:hover {
  transform: translateY(-4px) scale(1.01);
}

/* === Hover Glow === */
.hover-glow:hover .card-border-glow {
  opacity: 1;
}

/* === Interactive === */
.interactive {
  cursor: pointer;
}

/* === Header === */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.01em;
}

.card-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

/* === Body === */
.card-body {
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
}

/* === Footer === */
.card-footer {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* === Shine Effect === */
.card-shine {
  position: absolute;
  top: 0;
  left: -75%;
  width: 40%;
  height: 100%;
  background: linear-gradient(
    to right,
    transparent,
    rgba(102, 126, 234, 0.08),
    transparent
  );
  transform: skewX(-15deg);
  transition: left 0.6s ease;
  pointer-events: none;
}

.gradient-card:hover .card-shine {
  left: 125%;
}

/* === Border Glow Effect === */
.card-border-glow {
  position: absolute;
  inset: -2px;
  border-radius: 22px;
  background: linear-gradient(135deg, #667eea, #764ba2, #06b6d4, #667eea);
  background-size: 300% 300%;
  animation: gradient-rotate 4s ease infinite;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.4s ease;
}

@keyframes gradient-rotate {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* === Floating Particles === */
.card-floating-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: 20px;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  opacity: 0;
  transition: all 0.5s ease;
}

.gradient-card:hover .particle {
  animation: float-up 2s ease-in-out infinite;
}

.particle:nth-child(1) {
  left: 20%;
  bottom: 10%;
  animation-delay: 0s;
}

.particle:nth-child(2) {
  left: 50%;
  bottom: 15%;
  animation-delay: 0.3s;
}

.particle:nth-child(3) {
  left: 80%;
  bottom: 8%;
  animation-delay: 0.6s;
}

@keyframes float-up {
  0%, 100% {
    opacity: 0;
    transform: translateY(0) scale(0);
  }
  20% {
    opacity: 0.8;
    transform: translateY(-10px) scale(1);
  }
  80% {
    opacity: 0.3;
    transform: translateY(-60px) scale(0.5);
  }
  100% {
    opacity: 0;
    transform: translateY(-80px) scale(0);
  }
}
</style>
