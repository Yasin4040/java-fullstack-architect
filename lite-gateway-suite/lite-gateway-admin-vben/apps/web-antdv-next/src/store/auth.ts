import type { Recordable, UserInfo } from '@vben/types';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { resetAllStores, useAccessStore, useUserStore } from '@vben/stores';

import { notification } from 'antdv-next';
import { defineStore } from 'pinia';

import { getAccessCodesApi, getUserInfoApi, loginApi, logoutApi } from '#/api';
import { $t } from '#/locales';

export const useAuthStore = defineStore('auth', () => {
  const accessStore = useAccessStore();
  const userStore = useUserStore();
  const router = useRouter();

  const loginLoading = ref(false);

  /**
   * 异步处理登录操作
   * Asynchronously handle the login process
   * @param params 登录表单数据
   */
  async function authLogin(
    params: Recordable<any>,
    onSuccess?: () => Promise<void> | void,
  ) {
    // 异步处理用户登录操作并获取完整登录响应
    let userInfo: null | UserInfo = null;
    try {
      loginLoading.value = true;
      const loginResult = await loginApi(params as any);

      // 如果成功获取到登录响应
      if (loginResult?.accessToken) {
        // 保存 accessToken 和 refreshToken
        accessStore.setAccessToken(loginResult.accessToken);
        if (loginResult.refreshToken) {
          accessStore.setRefreshToken(loginResult.refreshToken);
        }

        // 直接使用登录响应中的 userInfo，无需额外请求
        // 将API返回的UserInfo转换为框架需要的UserInfo格式
        const apiUserInfo = loginResult.userInfo;
        const defaultHomePath = preferences.app.defaultHomePath;
        userInfo = {
          userId: String(apiUserInfo.userId),
          username: apiUserInfo.username,
          realName:
            apiUserInfo.realName ||
            apiUserInfo.nickname ||
            apiUserInfo.username,
          avatar: apiUserInfo.avatar || '',
          roles: apiUserInfo.roles || [],
          desc: apiUserInfo.nickname || apiUserInfo.username || '',
          homePath: defaultHomePath, // 使用项目配置的默认首页
          token: loginResult.accessToken,
        } as UserInfo;
        userStore.setUserInfo(userInfo);

        // 重置路由检查状态，让路由守卫重新生成路由
        accessStore.setIsAccessChecked(false);

        // 获取权限码（如果需要）
        try {
          const accessCodes = await getAccessCodesApi();
          accessStore.setAccessCodes(accessCodes);
        } catch (error) {
          // 如果获取权限码失败，设置为空数组
          console.warn('Failed to fetch access codes:', error);
          accessStore.setAccessCodes([]);
        }

        if (accessStore.loginExpired) {
          accessStore.setLoginExpired(false);
        } else {
          if (onSuccess) {
            await onSuccess?.();
          } else {
            // 跳转到默认首页，路由守卫会自动处理路由生成和跳转
            await router.replace(preferences.app.defaultHomePath);
          }
        }

        if (userInfo?.realName) {
          notification.success({
            description: `${$t('authentication.loginSuccessDesc')}:${userInfo?.realName}`,
            duration: 3,
            title: $t('authentication.loginSuccess'),
          });
        }
      }
    } catch (error) {
      // 登录失败时的错误处理已在请求拦截器中处理
      throw error;
    } finally {
      loginLoading.value = false;
    }

    return {
      userInfo,
    };
  }

  async function logout(redirect: boolean = true) {
    try {
      await logoutApi();
    } catch {
      // 不做任何处理
    }
    resetAllStores();
    accessStore.setLoginExpired(false);

    // 回登录页带上当前路由地址
    await router.replace({
      path: LOGIN_PATH,
      query: redirect
        ? {
            redirect: encodeURIComponent(router.currentRoute.value.fullPath),
          }
        : {},
    });
  }

  async function fetchUserInfo() {
    const apiUserInfo = await getUserInfoApi();
    // 确保 homePath 始终设置为默认首页
    const defaultHomePath = preferences.app.defaultHomePath;
    const userInfo: UserInfo = {
      userId: String(apiUserInfo.userId),
      username: apiUserInfo.username,
      realName:
        apiUserInfo.realName ||
        (apiUserInfo as any).nickname ||
        apiUserInfo.username,
      avatar: apiUserInfo.avatar || '',
      roles: apiUserInfo.roles || [],
      desc: (apiUserInfo as any).nickname || apiUserInfo.username || '',
      homePath: defaultHomePath, // 强制使用项目配置的默认首页
      token: accessStore.accessToken || '',
    } as UserInfo;
    userStore.setUserInfo(userInfo);
    return userInfo;
  }

  function $reset() {
    loginLoading.value = false;
  }

  return {
    $reset,
    authLogin,
    fetchUserInfo,
    loginLoading,
    logout,
  };
});
