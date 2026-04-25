<template>
  <div class="loader-wrapper" :class="`variant-${variant}`" :style="{ '--size': size + 'px' }">
    <div class="loader" :class="`type-${type}`">
      <!-- Spinner Type -->
      <template v-if="type === 'spinner'">
        <div class="spinner-ring"></div>
      </template>

      <!-- Dots Type -->
      <template v-else-if="type === 'dots'">
        <span v-for="i in 3" :key="i" class="dot" :style="{ '--i': i }"></span>
      </template>

      <!-- Pulse Type -->
      <template v-else-if="type === 'pulse'">
        <span v-for="i in 3" :key="i" class="pulse-bar" :style="{ '--i': i }"></span>
      </template>

      <!-- Orbit Type -->
      <template v-else-if="type === 'orbit'">
        <div class="orbit">
          <span class="orbit-dot"></span>
        </div>
      </template>

      <!-- Bounce Type -->
      <template v-else-if="type === 'bounce'">
        <span v-for="i in 3" :key="i" class="bounce-dot" :style="{ '--i': i }"></span>
      </template>

      <!-- Gradient Bar Type -->
      <template v-else-if="type === 'gradient-bar'">
        <div class="gradient-bar">
          <span class="bar-shine"></span>
        </div>
      </template>
    </div>
    
    <p v-if="text" class="loader-text">{{ text }}</p>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'spinner',
  },
  size: {
    type: Number,
    default: 40,
  },
  variant: {
    type: String,
    default: 'primary',
  },
  text: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
/* ================================================
   Gradient Loader — Galaxy Style
   Inspired by Uiverse.io Galaxy library
   ================================================ */

.loader-wrapper {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-family: 'Segoe UI', system-ui, sans-serif;
}

.loader {
  width: var(--size);
  height: var(--size);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* === Spinner === */
.spinner-ring {
  width: 100%;
  height: 100%;
  border: 3px solid rgba(102, 126, 234, 0.15);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.variant-secondary .spinner-ring {
  border-color: rgba(6, 182, 212, 0.15);
  border-top-color: #06b6d4;
}

.variant-success .spinner-ring {
  border-color: rgba(16, 185, 129, 0.15);
  border-top-color: #10b981;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* === Dots === */
.dot {
  width: calc(var(--size) * 0.25);
  height: calc(var(--size) * 0.25);
  margin: 0 calc(var(--size) * 0.08);
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: dot-bounce 1.4s ease-in-out infinite;
  animation-delay: calc(var(--i) * 0.16s);
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* === Pulse === */
.pulse-bar {
  width: calc(var(--size) * 0.15);
  height: 100%;
  margin: 0 calc(var(--size) * 0.05);
  border-radius: calc(var(--size) * 0.1);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: pulse-grow 1.3s ease-in-out infinite;
  animation-delay: calc(var(--i) * 0.12s);
}

@keyframes pulse-grow {
  0%, 100% {
    transform: scaleY(0.4);
  }
  50% {
    transform: scaleY(1);
  }
}

/* === Orbit === */
.orbit {
  width: 100%;
  height: 100%;
  border: 2px solid rgba(102, 126, 234, 0.15);
  border-radius: 50%;
  position: relative;
  animation: orbit-spin 1.5s linear infinite;
}

.orbit-dot {
  position: absolute;
  top: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: calc(var(--size) * 0.2);
  height: calc(var(--size) * 0.2);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(102, 126, 234, 0.5);
}

@keyframes orbit-spin {
  to { transform: rotate(360deg); }
}

/* === Bounce === */
.bounce-dot {
  width: calc(var(--size) * 0.3);
  height: calc(var(--size) * 0.3);
  margin: 0 calc(var(--size) * 0.05);
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: bounce-ball 1.4s ease-in-out infinite;
  animation-delay: calc(var(--i) * 0.15s);
}

@keyframes bounce-ball {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(calc(var(--size) * -0.4));
  }
}

/* === Gradient Bar === */
.gradient-bar {
  width: calc(var(--size) * 1.5);
  height: calc(var(--size) * 0.2);
  background: rgba(102, 126, 234, 0.15);
  border-radius: calc(var(--size) * 0.1);
  overflow: hidden;
  position: relative;
}

.bar-shine {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, transparent, #667eea, #764ba2, transparent);
  border-radius: inherit;
  animation: bar-slide 1.5s ease-in-out infinite;
}

@keyframes bar-slide {
  0% {
    left: -40%;
  }
  100% {
    left: 110%;
  }
}

/* === Text === */
.loader-text {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  letter-spacing: 0.05em;
}
</style>
