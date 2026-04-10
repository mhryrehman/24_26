/**
 * Dynamic Viewport Height Utility
 * Handles mobile browser toolbar issues by dynamically adjusting viewport height
 */

class ViewportHeightManager {
  private static instance: ViewportHeightManager;
  private resizeTimeout: number | null = null;
  private lastHeight: number = 0;

  private constructor() {
    this.init();
  }

  public static getInstance(): ViewportHeightManager {
    if (!ViewportHeightManager.instance) {
      ViewportHeightManager.instance = new ViewportHeightManager();
    }
    return ViewportHeightManager.instance;
  }

  private init(): void {
    this.setViewportHeight();
    this.addEventListeners();
  }

  private setViewportHeight(): void {
    // Get the actual viewport height
    const vh = window.innerHeight * 0.01;
    
    // Only update if height has changed significantly (more than 10px)
    if (Math.abs(window.innerHeight - this.lastHeight) > 10) {
      document.documentElement.style.setProperty('--vh', `${vh}px`);
      this.lastHeight = window.innerHeight;
    }
  }

  private addEventListeners(): void {
    // Handle resize events with debouncing
    window.addEventListener('resize', () => {
      if (this.resizeTimeout) {
        clearTimeout(this.resizeTimeout);
      }
      
      this.resizeTimeout = window.setTimeout(() => {
        this.setViewportHeight();
      }, 100);
    });

    // Handle orientation change
    window.addEventListener('orientationchange', () => {
      // Delay to ensure the viewport has adjusted
      setTimeout(() => {
        this.setViewportHeight();
      }, 500);
    });

    // Handle visual viewport changes (for mobile browsers)
    if ('visualViewport' in window) {
      window.visualViewport?.addEventListener('resize', () => {
        this.setViewportHeight();
      });
    }

    // Handle focus/blur events for input fields (mobile keyboard)
    document.addEventListener('focusin', () => {
      // Small delay to ensure keyboard is shown
      setTimeout(() => {
        this.setViewportHeight();
      }, 300);
    });

    document.addEventListener('focusout', () => {
      // Small delay to ensure keyboard is hidden
      setTimeout(() => {
        this.setViewportHeight();
      }, 300);
    });
  }

  public recalculate(): void {
    this.setViewportHeight();
  }
}

// Initialize the viewport height manager
export const initViewportHeight = (): ViewportHeightManager => {
  return ViewportHeightManager.getInstance();
};

// Export for manual recalculation if needed
export const recalculateViewportHeight = (): void => {
  ViewportHeightManager.getInstance().recalculate();
};
