// UI Component declarations for Gradient-* components
declare module '@/components/ui' {
  import type { DefineComponent } from 'vue'

  export const GradientButton: DefineComponent<Record<string, unknown>>
  export const GradientInput: DefineComponent<Record<string, unknown>>
  export const GradientCard: DefineComponent<Record<string, unknown>>
  export const GradientToggle: DefineComponent<Record<string, unknown>>
  export const GradientModal: DefineComponent<Record<string, unknown>>
  export const GradientCheckbox: DefineComponent<Record<string, unknown>>
  export const GradientLoader: DefineComponent<Record<string, unknown>>
}