import { createApi, type BaseQueryFn } from '@reduxjs/toolkit/query/react'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { axiosClient } from './axiosClient'
import type { ApiResponse } from './systemApi'

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

export interface LeadDto {
  id: string
  ownerUserId: string | null
  fullName: string
  email: string | null
  phone: string | null
  companyName: string | null
  jobTitle: string | null
  source: string
  status: string
  leadScore: number
  estimatedValue: number | null
  notes: string | null
  convertedAccountId: string | null
  convertedContactId: string | null
  convertedAt: string | null
  createdAt: string
}

export interface AccountDto {
  id: string
  parentAccountId: string | null
  name: string
  industry: string | null
  website: string | null
  phone: string | null
  annualRevenue: number | null
  employeeCount: number | null
  lifecycleStage: string
  billingCity: string | null
  billingCountry: string | null
  createdAt: string
}

export interface ContactDto {
  id: string
  accountId: string | null
  firstName: string
  lastName: string
  email: string | null
  phone: string | null
  jobTitle: string | null
  department: string | null
  segment: string | null
  createdAt: string
}

export interface StageDto {
  id: string
  name: string
  probability: number
  stageOrder: number
  closed: boolean
  won: boolean
}

export interface PipelineDto {
  id: string
  name: string
  defaultPipeline: boolean
  stages: StageDto[]
}

export interface DealDto {
  id: string
  pipelineId: string
  stageId: string
  stageName: string
  closed: boolean
  won: boolean
  accountId: string | null
  contactId: string | null
  title: string
  amount: number
  probability: number
  expectedRevenue: number
  expectedCloseDate: string | null
  lostReason: string | null
  createdAt: string
}

export interface CrmDashboardDto {
  totalLeads: number
  convertedLeads: number
  totalAccounts: number
  totalContacts: number
  openDeals: number
  openPipelineAmount: number
  weightedForecast: number
  closedWonAmount: number
  stageBreakdown: { stageId: string; stageName: string; dealCount: number; totalAmount: number }[]
  leadsBySource: { source: string; leadCount: number }[]
}

export interface ProductDto {
  id: string
  sku: string
  name: string
  description: string | null
  category: string | null
  unitPrice: number
  taxRate: number
  active: boolean
}

export interface QuoteLineDto {
  id: string
  productId: string | null
  itemName: string
  quantity: number
  unitPrice: number
  discountPercent: number
  taxRate: number
  lineTotal: number
}

export interface QuoteDto {
  id: string
  quoteNumber: string
  accountId: string | null
  contactId: string | null
  dealId: string | null
  subject: string
  status: string
  validUntil: string | null
  subtotal: number
  taxTotal: number
  grandTotal: number
  notes: string | null
  lines: QuoteLineDto[]
  createdAt: string
}

export interface SalesOrderDto {
  id: string
  orderNumber: string
  quoteId: string | null
  accountId: string | null
  dealId: string | null
  subject: string
  status: string
  orderDate: string
  grandTotal: number
  createdAt: string
}

export interface ActivityDto {
  id: string
  activityType: string
  subject: string
  description: string | null
  relatedEntityType: string
  relatedEntityId: string
  dueAt: string | null
  completedAt: string | null
  status: string
  createdAt: string
}

export const crmApi = createApi({
  reducerPath: 'crmApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['CrmDashboard', 'Leads', 'Accounts', 'Contacts', 'Pipelines', 'Deals', 'Products', 'Quotes', 'Orders', 'Activities'],
  endpoints: (builder) => ({
    getCrmDashboard: builder.query<CrmDashboardDto, void>({
      query: () => ({ url: '/crm/dashboard' }),
      transformResponse: (res: ApiResponse<CrmDashboardDto>) => res.data,
      providesTags: ['CrmDashboard'],
    }),
    getLeads: builder.query<ApiResponse<LeadDto[]>, { status?: string; q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/crm/leads', params: params || { page: 0, size: 50 } }),
      providesTags: ['Leads'],
    }),
    createLead: builder.mutation<
      LeadDto,
      { fullName: string; email?: string; phone?: string; companyName?: string; source?: string; estimatedValue?: number }
    >({
      query: (data) => ({ url: '/crm/leads', method: 'POST', data }),
      transformResponse: (res: ApiResponse<LeadDto>) => res.data,
      invalidatesTags: ['Leads', 'CrmDashboard'],
    }),
    convertLead: builder.mutation<
      { leadId: string; accountId: string; contactId: string; dealId: string | null },
      { id: string; createDeal: boolean; dealTitle?: string; dealAmount?: number }
    >({
      query: ({ id, ...data }) => ({ url: `/crm/leads/${id}/convert`, method: 'POST', data }),
      transformResponse: (res: ApiResponse<{ leadId: string; accountId: string; contactId: string; dealId: string | null }>) =>
        res.data,
      invalidatesTags: ['Leads', 'Accounts', 'Contacts', 'Deals', 'CrmDashboard', 'Activities'],
    }),
    archiveLead: builder.mutation<void, string>({
      query: (id) => ({ url: `/crm/leads/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Leads', 'CrmDashboard'],
    }),
    importLeadsCsv: builder.mutation<{ imported: number; skipped: number }, { csv: string }>({
      query: (data) => ({ url: '/crm/leads/import', method: 'POST', data }),
      transformResponse: (res: ApiResponse<{ imported: number; skipped: number }>) => res.data,
      invalidatesTags: ['Leads', 'CrmDashboard'],
    }),

    getAccounts: builder.query<ApiResponse<AccountDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/crm/accounts', params: params || { page: 0, size: 50 } }),
      providesTags: ['Accounts'],
    }),
    createAccount: builder.mutation<
      AccountDto,
      { name: string; industry?: string; website?: string; annualRevenue?: number; lifecycleStage?: string }
    >({
      query: (data) => ({ url: '/crm/accounts', method: 'POST', data }),
      transformResponse: (res: ApiResponse<AccountDto>) => res.data,
      invalidatesTags: ['Accounts', 'CrmDashboard'],
    }),

    getContacts: builder.query<ApiResponse<ContactDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/crm/contacts', params: params || { page: 0, size: 50 } }),
      providesTags: ['Contacts'],
    }),
    createContact: builder.mutation<
      ContactDto,
      { accountId?: string; firstName: string; lastName: string; email?: string; jobTitle?: string }
    >({
      query: (data) => ({ url: '/crm/contacts', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ContactDto>) => res.data,
      invalidatesTags: ['Contacts', 'CrmDashboard'],
    }),

    getPipelines: builder.query<PipelineDto[], void>({
      query: () => ({ url: '/crm/pipelines' }),
      transformResponse: (res: ApiResponse<PipelineDto[]>) => res.data,
      providesTags: ['Pipelines'],
    }),
    getDeals: builder.query<ApiResponse<DealDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/crm/deals', params: params || { page: 0, size: 100 } }),
      providesTags: ['Deals'],
    }),
    createDeal: builder.mutation<
      DealDto,
      { title: string; amount: number; accountId?: string; stageId?: string; expectedCloseDate?: string }
    >({
      query: (data) => ({ url: '/crm/deals', method: 'POST', data }),
      transformResponse: (res: ApiResponse<DealDto>) => res.data,
      invalidatesTags: ['Deals', 'CrmDashboard'],
    }),
    moveDealStage: builder.mutation<DealDto, { id: string; stageId: string; lostReason?: string }>({
      query: ({ id, ...data }) => ({ url: `/crm/deals/${id}/stage`, method: 'PATCH', data }),
      transformResponse: (res: ApiResponse<DealDto>) => res.data,
      invalidatesTags: ['Deals', 'CrmDashboard'],
    }),

    getProducts: builder.query<ApiResponse<ProductDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/crm/products', params: params || { page: 0, size: 50 } }),
      providesTags: ['Products'],
    }),
    createProduct: builder.mutation<
      ProductDto,
      { sku: string; name: string; category?: string; unitPrice: number; taxRate: number }
    >({
      query: (data) => ({ url: '/crm/products', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ProductDto>) => res.data,
      invalidatesTags: ['Products'],
    }),

    getQuotes: builder.query<ApiResponse<QuoteDto[]>, { page?: number; size?: number } | void>({
      query: (params) => ({ url: '/crm/quotes', params: params || { page: 0, size: 50 } }),
      providesTags: ['Quotes'],
    }),
    createQuote: builder.mutation<
      QuoteDto,
      {
        accountId?: string
        dealId?: string
        subject: string
        lines: { productId?: string; itemName: string; quantity: number; unitPrice: number; discountPercent: number; taxRate: number }[]
      }
    >({
      query: (data) => ({ url: '/crm/quotes', method: 'POST', data }),
      transformResponse: (res: ApiResponse<QuoteDto>) => res.data,
      invalidatesTags: ['Quotes'],
    }),
    convertQuoteToOrder: builder.mutation<SalesOrderDto, string>({
      query: (quoteId) => ({ url: `/crm/quotes/${quoteId}/convert-to-order`, method: 'POST' }),
      transformResponse: (res: ApiResponse<SalesOrderDto>) => res.data,
      invalidatesTags: ['Quotes', 'Orders'],
    }),
    getSalesOrders: builder.query<ApiResponse<SalesOrderDto[]>, void>({
      query: () => ({ url: '/crm/sales-orders', params: { page: 0, size: 50 } }),
      providesTags: ['Orders'],
    }),

    getActivities: builder.query<ApiResponse<ActivityDto[]>, void>({
      query: () => ({ url: '/crm/activities', params: { page: 0, size: 50 } }),
      providesTags: ['Activities'],
    }),
    createActivity: builder.mutation<
      ActivityDto,
      { activityType: string; subject: string; relatedEntityType: string; relatedEntityId: string }
    >({
      query: (data) => ({ url: '/crm/activities', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ActivityDto>) => res.data,
      invalidatesTags: ['Activities'],
    }),
    completeActivity: builder.mutation<ActivityDto, string>({
      query: (id) => ({ url: `/crm/activities/${id}/complete`, method: 'POST' }),
      transformResponse: (res: ApiResponse<ActivityDto>) => res.data,
      invalidatesTags: ['Activities'],
    }),
  }),
})

export const {
  useGetCrmDashboardQuery,
  useGetLeadsQuery,
  useCreateLeadMutation,
  useConvertLeadMutation,
  useArchiveLeadMutation,
  useImportLeadsCsvMutation,
  useGetAccountsQuery,
  useCreateAccountMutation,
  useGetContactsQuery,
  useCreateContactMutation,
  useGetPipelinesQuery,
  useGetDealsQuery,
  useCreateDealMutation,
  useMoveDealStageMutation,
  useGetProductsQuery,
  useCreateProductMutation,
  useGetQuotesQuery,
  useCreateQuoteMutation,
  useConvertQuoteToOrderMutation,
  useGetSalesOrdersQuery,
  useGetActivitiesQuery,
  useCreateActivityMutation,
  useCompleteActivityMutation,
} = crmApi

