import { baseRequestClient, requestClient } from '#/api/request';

import type { LoginParams, LoginResult } from '#/types/api';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    username: string;
    password: string;
    captcha?: string;
    captchaKey?: string;
    selectAccount?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    userInfo: {
      userId: number;
      username: string;
      nickname?: string;
      realName?: string;
      email?: string;
      phone?: string;
      avatar?: string;
      roles: string[];
      permissions: string[];
    };
  }

  export interface RefreshTokenResult {
    accessToken: string;
    refreshToken?: string;
    tokenType?: string;
    expiresIn?: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: LoginParams | AuthApi.LoginParams) {
  return requestClient.post<LoginResult>('/gateway/auth/login', data);
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi(refreshToken?: string) {
  // 如果没有传入refreshToken，尝试从store中获取
  if (!refreshToken) {
    const { useAccessStore } = await import('@vben/stores');
    const accessStore = useAccessStore();
    refreshToken = accessStore.refreshToken;
  }

  if (!refreshToken) {
    throw new Error('Refresh token is not available');
  }

  // 发送refreshToken到请求体
  // 使用requestClient以便使用响应拦截器处理响应格式
  // 标记为刷新token请求，避免触发token刷新逻辑
  return requestClient.post<AuthApi.LoginResult>('/gateway/auth/refresh', {
    refreshToken,
  }, {
    __isRetryRequest: true, // 标记为刷新token请求，避免循环
  } as any);
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return baseRequestClient.post('/gateway/auth/logout', {
    withCredentials: true,
  });
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/gateway/auth/codes');
}
