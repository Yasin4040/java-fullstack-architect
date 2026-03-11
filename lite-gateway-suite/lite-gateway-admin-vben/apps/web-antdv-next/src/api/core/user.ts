import type { UserInfo as ApiUserInfo } from '#/types/api';

import { requestClient } from '#/api/request';

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  return requestClient.get<ApiUserInfo>('/gateway/auth/info');
}
