import { createApi, type BaseQueryFn } from '@reduxjs/toolkit/query/react'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { axiosClient } from './axiosClient'

interface AxiosBaseQueryArgs {
  url: string
  method?: AxiosRequestConfig['method']
  data?: AxiosRequestConfig['data']
  params?: AxiosRequestConfig['params']
}

interface AxiosBaseQueryError {
  status?: number
  data?: unknown
}

const axiosBaseQuery =
  (): BaseQueryFn<AxiosBaseQueryArgs, unknown, AxiosBaseQueryError> =>
  async ({ url, method = 'GET', data, params }) => {
    try {
      const result = await axiosClient.request({ url, method, data, params })
      return { data: result.data }
    } catch (err) {
      const error = err as AxiosError
      return {
        error: {
          status: error.response?.status,
          data: error.response?.data || error.message,
        },
      }
    }
  }

export interface ApiMeta {
  timestamp: string
  page?: number
  size?: number
  totalElements?: number
  totalPages?: number
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  meta: ApiMeta
}

export interface PublicSetting {
  key: string
  value: string
  category: string
  description: string
}

export interface SystemStatus {
  platformName: string
  version: string
  currentPhase: number
  status: string
  databaseProduct: string
  totalModules: number
  activeModules: number
  plannedModules: number
  serverTime: string
  publicSettings: PublicSetting[]
}

export interface PlatformModule {
  id: string
  code: string
  name: string
  tagline: string
  description: string
  category: string
  phaseNumber: number
  status: 'ACTIVE' | 'PLANNED' | string
  routePath: string
  iconName: string
  sortOrder: number
  boundedContext: string
}

export interface ModuleFilterParams {
  category?: string
  phase?: number
  q?: string
  page?: number
  size?: number
}

export const systemApi = createApi({
  reducerPath: 'systemApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['SystemStatus', 'PlatformModule'],
  endpoints: (builder) => ({
    getSystemStatus: builder.query<SystemStatus, void>({
      query: () => ({ url: '/system/status' }),
      transformResponse: (response: ApiResponse<SystemStatus>) => response.data,
      providesTags: ['SystemStatus'],
    }),
    getModules: builder.query<ApiResponse<PlatformModule[]>, ModuleFilterParams | void>({
      query: (params) => ({
        url: '/system/modules',
        params: params || { page: 0, size: 50 },
      }),
      providesTags: ['PlatformModule'],
    }),
    getModuleByCode: builder.query<PlatformModule, string>({
      query: (code) => ({ url: `/system/modules/${code}` }),
      transformResponse: (response: ApiResponse<PlatformModule>) => response.data,
      providesTags: (_result, _err, code) => [{ type: 'PlatformModule', id: code }],
    }),
  }),
})

export const { useGetSystemStatusQuery, useGetModulesQuery, useGetModuleByCodeQuery } = systemApi

