<template>
  <div class="input-wrapper" :class="{ focused: isFocused, 'has-error': error, 'has-value': modelValue }">
    <label v-if="label" :for="inputId" class="input-label" :class="{ floating: isFocused || modelValue }">
      {{ label }}
    </label>
    <div class="input-container">
      <div class="input-glow" aria-hidden="true"></div>
      <input
        :id="inputId"
        v-bind="$attrs"
        :type="type"
        :placeholder="isFocused ? placeholder : ''"
        :disabled="disabled"
        :value="modelValue"
        class="gradient-input"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <div class="input-underline" aria-hidden="true"></div>
      <div class="input-border-anim" aria-hidden="true"></div>
    </div>
    <div class="input-footer">
      <p v-if="error" class="error-msg">{{ error }}</p>
      <p v-else-if="hint" class="hint-msg">{{ hint }}</p>
      <span v-if="maxLength" class="char-count" :class="{ 'near-limit': modelValue.length > maxLength * 0.8 }">
        {{ modelValue.length }}/{{ maxLength }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  label: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: '',
  },
  type: {
    type: String,
    default: 'text',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  hint: {
    type: String,
    default: '',
  },
  maxLength: {
    type: Number,
    default: null,
  },
})

defineEmits(['update:modelValue'])

const isFocused = ref(false)
const inputId = `input-${Math.random().toString(36).slice(2, 8)}`
</script>

<style scoped>
/* ================================================
   Gradient Input — Enhanced Galaxy Style
   Inspired by Uiverse.io Galaxy library
   Enhanced with floating labels and micro-interactions
   ================================================ */

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  max-width: 400px;
  font-family: 'Segoe UI', system-ui, sans-serif;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.input-label.floating {
  font-size: 11px;
  color: #667eea;
  transform: translateY(-2px);
}

.focused .input-label {
  color: #667eea;
}

/* === Container === */
.input-container {
  position: relative;
  width: 100%;
}

.gradient-input {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 16px;
  font-size: 15px;
  font-family: inherit;
  color: #1e293b;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 14px;
  outline: none;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1.5px solid transparent;
  background-image: linear-gradient(white, white),
                    linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.gradient-input::placeholder {
  color: #94a3b8;
  transition: color 0.3s ease;
}

.gradient-input:focus::placeholder {
  color: #cbd5e1;
}

.gradient-input:hover:not(:disabled) {
  background-image: linear-gradient(#f8fafc, #f8fafc),
                    linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* === Underline Effect === */
.input-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #667eea, #764ba2, transparent);
  border-radius: 2px;
  transition: width 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  pointer-events: none;
}

.focused .input-underline {
  width: 90%;
}

/* === Border Animation === */
.input-border-anim {
  position: absolute;
  inset: 0;
  border-radius: 14px;
  border: 2px solid transparent;
  background: linear-gradient(135deg, #667eea, #764ba2, #06b6d4, #667eea) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.focused .input-border-anim {
  opacity: 1;
  animation: border-rotate 3s linear infinite;
}

@keyframes border-rotate {
  0% { filter: hue-rotate(0deg); }
  100% { filter: hue-rotate(360deg); }
}

/* === Glow Effect === */
.input-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 0;
  height: 0;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  transition: all 0.4s ease;
  pointer-events: none;
}

.focused .input-glow {
  width: 110%;
  height: 150%;
}

/* === Error State === */
.has-error .gradient-input {
  background-image: linear-gradient(white, white),
                    linear-gradient(135deg, #ef4444 0%, #f97316 100%);
}

.has-error .input-label {
  color: #ef4444;
}

.has-error .input-underline {
  background: linear-gradient(90deg, transparent, #ef4444, #f97316, transparent);
}

/* === Disabled === */
.gradient-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-image: linear-gradient(#f8fafc, #f8fafc),
                    linear-gradient(135deg, #cbd5e1 0%, #e2e8f0 100%);
}

/* === Footer === */
.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 18px;
}

.error-msg {
  font-size: 12px;
  color: #ef4444;
  margin: 0;
}

.hint-msg {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.char-count {
  font-size: 11px;
  color: #94a3b8;
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.char-count.near-limit {
  color: #f97316;
}
</style>
