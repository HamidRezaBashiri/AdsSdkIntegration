import { NativeModules } from 'react-native';

// Type definitions for ad results
export interface AdInitializationResult {
  status: 'success' | 'error';
}

export interface AdLoadResult {
  status: 'success' | 'error';
  message?: string;
  adUnitId?: string;
}

/**
 * React Native bridge for Google Ad SDK
 */
export class GoogleAdSDK {
  private static module = NativeModules.GoogleAdSDKModule;

  /**
   * Initialize the Google Ad SDK
   * @returns Promise resolving to initialization status
   */
  static async initialize(): Promise<boolean> {
    if (!this.module) {
      throw new Error('GoogleAdSDKModule is not available');
    }

    try {
      return await this.module.initialize();
    } catch (error) {
      console.error('Google Ad SDK Initialization Error:', error);
      throw error;
    }
  }

  /**
   * Load an ad for a specific ad unit ID
   * @param adUnitId The unique identifier for the ad placement
   * @returns Promise resolving to ad loading result
   */
  static async loadAd(adUnitId: string): Promise<AdLoadResult> {
    if (!this.module) {
      throw new Error('GoogleAdSDKModule is not available');
    }

    try {
      return await this.module.loadAd(adUnitId);
    } catch (error) {
      console.error('Google Ad Loading Error:', error);
      throw error;
    }
  }

  /**
   * Check if the Google Ad SDK is initialized
   * @returns Promise resolving to initialization status
   */
  static async isInitialized(): Promise<boolean> {
    if (!this.module) {
      throw new Error('GoogleAdSDKModule is not available');
    }

    try {
      return await this.module.isInitialized();
    } catch (error) {
      console.error('Google Ad SDK Initialization Check Error:', error);
      throw error;
    }
  }
}