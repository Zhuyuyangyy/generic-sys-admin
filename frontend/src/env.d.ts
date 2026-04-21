// Vue SFC component declarations
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

// UI component module declaration
declare module '@/components/ui' {
  import type { DefineComponent } from 'vue'

  export const GradientButton: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const GradientInput: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const GradientCard: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const GradientToggle: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const GradientModal: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const GradientCheckbox: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const GradientLoader: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
}