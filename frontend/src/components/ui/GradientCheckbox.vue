<template>
  <label class="checkbox-wrapper" :class="{ checked: modelValue, disabled, indeterminate }">
    <input
      type="checkbox"
      class="checkbox-input"
      :checked="modelValue"
      :disabled="disabled"
      @change="handleChange"
    />
    <span class="checkbox-box" aria-hidden="true">
      <svg class="checkmark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
      <span class="checkbox-glow"></span>
    </span>
    <span v-if="label" class="checkbox-label">{{ label }}</span>
  </label>
</template>

<script setup>
const props = defineProps({
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
  indeterminate: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

function handleChange(event) {
  emit('update:modelValue', event.target.checked)
}
</script>

<style scoped>
/* ================================================
   Gradient Checkbox — Galaxy Style
   Inspired by Uiverse.io Galaxy library
   ================================================ */

.checkbox-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-family: 'Segoe UI', system-ui, sans-serif;
  user-select: none;
}

.checkbox-wrapper.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.checkbox-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.checkbox-box {
  position: relative;
  width: 22px;
  height: 22px;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 6px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.checkbox-glow {
  position: absolute;
  inset: -4px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  opacity: 0;
  filter: blur(8px);
  transition: opacity 0.3s ease;
  z-index: -1;
}

.checkmark {
  width: 14px;
  height: 14px;
  color: #ffffff;
  opacity: 0;
  transform: scale(0.5);
  transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* Checked state */
.checked .checkbox-box {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.35);
}

.checked .checkmark {
  opacity: 1;
  transform: scale(1);
}

.checked .checkbox-glow {
  opacity: 0.4;
}

/* Hover */
.checkbox-wrapper:hover:not(.disabled) .checkbox-box {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.checked:hover:not(.disabled) .checkbox-box {
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.5);
}

/* Focus */
.checkbox-input:focus-visible + .checkbox-box {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.3);
}

.checkbox-label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  transition: color 0.3s ease;
}

.checked .checkbox-label {
  color: #1e293b;
}
</style>
