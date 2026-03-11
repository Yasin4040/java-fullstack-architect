/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { RequestClientOptions } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { message } from 'antdv-next';

import { useAuthStore } from '#/store';

import { refreshTokenApi } from './core';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const resp = await refreshTokenApi(accessStore.refreshToken);
    
    // 更新accessToken和refreshToken
    accessStore.setAccessToken(resp.accessToken);
    if (resp.refreshToken) {
      accessStore.setRefreshToken(resp.refreshToken);
    }
    
    return resp.accessToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();

      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: '00000',
    }),
  );

  // 自定义拦截器：处理业务错误码（如B0001表示token过期）
  // 这个拦截器需要在defaultResponseInterceptor之后执行，以便能访问到响应数据
  client.addResponseInterceptor({
    fulfilled: (response) => {
      // 检查响应数据中的业务错误码
      const responseData = response?.data ?? {};
      const errorCode = responseData?.code;
      
      // 如果响应成功（code为00000），直接返回
      if (errorCode === '00000' || !errorCode) {
        return response;
      }
      
      // 检查是否是token过期相关的错误码
      // A0230: 用户登录已过期
      // A0231: 用户未登录/认证令牌无效
      // B0001: 系统执行出错（只有当错误消息明确表示token过期时才视为token过期）
      const errorMessage = typeof responseData?.message === 'string' ? responseData.message : '';
      const isTokenExpiredError = 
        errorCode === 'A0230' ||  // 用户登录已过期
        errorCode === 'A0231' ||  // 用户未登录/认证令牌无效
        errorCode === '401' ||
        (errorCode === 'B0001' && errorMessage && (
          errorMessage.includes('过期') || 
          errorMessage.includes('expired') ||
          errorMessage.includes('JWT expired') ||
          errorMessage.includes('登录已过期') ||
          errorMessage.includes('认证令牌无效') ||
          errorMessage.includes('未提供认证令牌') ||
          errorMessage.includes('Token expired')
        )) ||
        (errorMessage && (
          errorMessage.includes('过期') || 
          errorMessage.includes('expired') ||
          errorMessage.includes('JWT expired') ||
          errorMessage.includes('登录已过期') ||
          errorMessage.includes('认证令牌无效') ||
          errorMessage.includes('未提供认证令牌') ||
          errorMessage.includes('Token expired')
        ));

      // 如果是token过期错误，抛出特殊错误以便后续拦截器处理
      if (isTokenExpiredError) {
        const error: any = new Error(responseData?.message || 'Token expired');
        error.response = {
          ...response,
          status: 401,
          data: responseData,
        };
        error.config = response.config;
        throw error;
      }
      
      return response;
    },
    rejected: async (error) => {
      const { config, response } = error;
      const responseData = response?.data ?? {};
      const errorCode = responseData?.code;
      
      // 检查是否是token过期相关的错误码
      // A0230: 用户登录已过期
      // A0231: 用户未登录/认证令牌无效
      // B0001: 系统执行出错（只有当错误消息明确表示token过期时才视为token过期）
      const errorMessage = typeof responseData?.message === 'string' ? responseData.message : '';
      const isTokenExpiredError = 
        errorCode === 'A0230' ||  // 用户登录已过期
        errorCode === 'A0231' ||  // 用户未登录/认证令牌无效
        errorCode === '401' ||
        response?.status === 401 ||
        (errorCode === 'B0001' && errorMessage && (
          errorMessage.includes('过期') || 
          errorMessage.includes('expired') ||
          errorMessage.includes('JWT expired') ||
          errorMessage.includes('登录已过期') ||
          errorMessage.includes('认证令牌无效') ||
          errorMessage.includes('未提供认证令牌') ||
          errorMessage.includes('Token expired')
        )) ||
        (errorMessage && (
          errorMessage.includes('过期') || 
          errorMessage.includes('expired') ||
          errorMessage.includes('JWT expired') ||
          errorMessage.includes('登录已过期') ||
          errorMessage.includes('认证令牌无效') ||
          errorMessage.includes('未提供认证令牌') ||
          errorMessage.includes('Token expired')
        ));

      // 如果是token过期错误，且不是刷新token的请求，尝试刷新token
      if (isTokenExpiredError && config && !config.__isRetryRequest) {
        // 检查是否启用了refreshToken功能
        if (preferences.app.enableRefreshToken && !client.isRefreshing) {
          try {
            // 标记开始刷新token
            client.isRefreshing = true;
            config.__isRetryRequest = true;

            const newToken = await doRefreshToken();

            // 处理队列中的请求
            if (client.refreshTokenQueue && client.refreshTokenQueue.length > 0) {
              client.refreshTokenQueue.forEach((callback) => callback(newToken));
              client.refreshTokenQueue = [];
            }

            // 更新请求头中的token并重试请求
            config.headers = config.headers || {};
            config.headers.Authorization = formatToken(newToken);
            return client.request(config.url || '', { ...config });
          } catch (refreshError) {
            // 如果刷新token失败，处理错误
            if (client.refreshTokenQueue) {
              client.refreshTokenQueue.forEach((callback) => callback(''));
              client.refreshTokenQueue = [];
            }
            console.error('Refresh token failed, please login again.');
            await doReAuthenticate();
            throw refreshError;
          } finally {
            client.isRefreshing = false;
          }
        } else if (client.isRefreshing) {
          // 如果正在刷新token，将请求加入队列
          return new Promise((resolve, reject) => {
            if (!client.refreshTokenQueue) {
              client.refreshTokenQueue = [];
            }
            client.refreshTokenQueue.push((newToken: string) => {
              if (newToken) {
                config.headers = config.headers || {};
                config.headers.Authorization = formatToken(newToken);
                client.request(config.url || '', { ...config })
                  .then(resolve)
                  .catch(reject);
              } else {
                reject(error);
              }
            });
          });
        } else {
          // 无法刷新token，需要重新登录
          await doReAuthenticate();
        }
      }

      // 其他错误继续抛出
      throw error;
    },
  });

  // token过期的处理（HTTP 401状态码）
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: preferences.app.enableRefreshToken,
      formatToken,
    }),
  );

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      // 根据API文档，错误响应格式为 { code: string, message: string, data: any }
      // 优先使用响应中的 message 字段
      const responseData = error?.response?.data ?? {};
      const errorMessage = responseData?.message ?? responseData?.error ?? msg;
      // 显示错误提示
      message.error(errorMessage);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

export const baseRequestClient = new RequestClient({ baseURL: apiURL });
