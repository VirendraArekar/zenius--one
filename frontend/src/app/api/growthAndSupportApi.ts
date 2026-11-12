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

export interface MarketingDashboardDto {
  subscribedCount: number
  totalCampaigns: number
  sentCampaigns: number
  totalDispatched: number
  totalOpens: number
  totalClicks: number
  totalFormSubmissions: number
  activeCoupons: number
}

export interface SubscriberDto {
  id: string
  email: string
  fullName: string | null
  source: string
  tags: string | null
  status: string
  createdAt: string
}

export interface SegmentDto {
  id: string
  name: string
  description: string | null
  filterTag: string | null
  filterSource: string | null
  matchingSubscribers: number
}

export interface CampaignDto {
  id: string
  segmentId: string | null
  name: string
  channel: string
  subject: string
  status: string
  sentAt: string | null
  sentCount: number
  openedCount: number
  clickedCount: number
  createdAt: string
}

export interface JourneyDto {
  id: string
  name: string
  triggerEvent: string
  stepsSummary: string
  status: string
  enrolledCount: number
}

export interface FormDto {
  id: string
  name: string
  title: string
  submitLabel: string
  autoCreateLead: boolean
  submissionsCount: number
  status: string
}

export interface CouponDto {
  id: string
  code: string
  discountPercent: number
  maxRedemptions: number | null
  redemptionCount: number
  validUntil: string | null
  active: boolean
}

export interface SupportDashboardDto {
  totalTickets: number
  openTickets: number
  resolvedTickets: number
  escalatedTickets: number
  averageCsat: number
  publishedKbArticles: number
  fieldAppointments: number
}

export interface SlaPolicyDto {
  id: string
  name: string
  priority: string
  firstResponseMinutes: number
  resolutionMinutes: number
}

export interface TicketDto {
  id: string
  ticketNumber: string
  subject: string
  description: string
  channel: string
  priority: string
  status: string
  requesterName: string
  requesterEmail: string
  slaDueAt: string | null
  escalated: boolean
  csatRating: number | null
  createdAt: string
}

export interface TicketCommentDto {
  id: string
  ticketId: string
  authorName: string
  body: string
  internalNote: boolean
  createdAt: string
}

export interface KbArticleDto {
  id: string
  title: string
  category: string
  content: string
  published: boolean
  helpfulCount: number
  createdAt: string
}

export interface AppointmentDto {
  id: string
  ticketId: string | null
  appointmentNumber: string
  customerName: string
  serviceAddress: string
  scheduledStart: string
  status: string
}

export const growthAndSupportApi = createApi({
  reducerPath: 'growthAndSupportApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: [
    'MktDashboard',
    'Subscribers',
    'Segments',
    'Campaigns',
    'Journeys',
    'Forms',
    'Coupons',
    'SupDashboard',
    'Tickets',
    'Comments',
    'KbArticles',
    'Appointments',
  ],
  endpoints: (builder) => ({
    // Marketing
    getMarketingDashboard: builder.query<MarketingDashboardDto, void>({
      query: () => ({ url: '/marketing/dashboard' }),
      transformResponse: (res: ApiResponse<MarketingDashboardDto>) => res.data,
      providesTags: ['MktDashboard'],
    }),
    getSubscribers: builder.query<ApiResponse<SubscriberDto[]>, { q?: string } | void>({
      query: (params) => ({ url: '/marketing/subscribers', params: params || { page: 0, size: 50 } }),
      providesTags: ['Subscribers'],
    }),
    upsertSubscriber: builder.mutation<SubscriberDto, { email: string; fullName?: string; source?: string; tags?: string }>({
      query: (data) => ({ url: '/marketing/subscribers', method: 'POST', data }),
      transformResponse: (res: ApiResponse<SubscriberDto>) => res.data,
      invalidatesTags: ['Subscribers', 'Segments', 'MktDashboard'],
    }),
    getSegments: builder.query<SegmentDto[], void>({
      query: () => ({ url: '/marketing/segments' }),
      transformResponse: (res: ApiResponse<SegmentDto[]>) => res.data,
      providesTags: ['Segments'],
    }),
    createSegment: builder.mutation<SegmentDto, { name: string; filterTag?: string; filterSource?: string }>({
      query: (data) => ({ url: '/marketing/segments', method: 'POST', data }),
      transformResponse: (res: ApiResponse<SegmentDto>) => res.data,
      invalidatesTags: ['Segments'],
    }),
    getCampaigns: builder.query<ApiResponse<CampaignDto[]>, void>({
      query: () => ({ url: '/marketing/campaigns', params: { page: 0, size: 50 } }),
      providesTags: ['Campaigns'],
    }),
    createCampaign: builder.mutation<
      CampaignDto,
      { segmentId?: string; name: string; subject: string; bodyContent: string }
    >({
      query: (data) => ({ url: '/marketing/campaigns', method: 'POST', data }),
      transformResponse: (res: ApiResponse<CampaignDto>) => res.data,
      invalidatesTags: ['Campaigns', 'MktDashboard'],
    }),
    launchCampaign: builder.mutation<CampaignDto, string>({
      query: (id) => ({ url: `/marketing/campaigns/${id}/launch`, method: 'POST' }),
      transformResponse: (res: ApiResponse<CampaignDto>) => res.data,
      invalidatesTags: ['Campaigns', 'MktDashboard'],
    }),
    getJourneys: builder.query<JourneyDto[], void>({
      query: () => ({ url: '/marketing/journeys' }),
      transformResponse: (res: ApiResponse<JourneyDto[]>) => res.data,
      providesTags: ['Journeys'],
    }),
    createJourney: builder.mutation<JourneyDto, { name: string; triggerEvent: string; stepsSummary: string }>({
      query: (data) => ({ url: '/marketing/journeys', method: 'POST', data }),
      transformResponse: (res: ApiResponse<JourneyDto>) => res.data,
      invalidatesTags: ['Journeys'],
    }),
    getForms: builder.query<FormDto[], void>({
      query: () => ({ url: '/marketing/forms' }),
      transformResponse: (res: ApiResponse<FormDto[]>) => res.data,
      providesTags: ['Forms'],
    }),
    createForm: builder.mutation<FormDto, { name: string; title: string; autoCreateLead: boolean }>({
      query: (data) => ({ url: '/marketing/forms', method: 'POST', data }),
      transformResponse: (res: ApiResponse<FormDto>) => res.data,
      invalidatesTags: ['Forms'],
    }),
    submitMarketingForm: builder.mutation<
      { formId: string; createdLeadId: string | null },
      { formId: string; fullName: string; email: string; companyName?: string }
    >({
      query: ({ formId, ...data }) => ({ url: `/marketing/forms/${formId}/submit`, method: 'POST', data }),
      transformResponse: (res: ApiResponse<{ formId: string; createdLeadId: string | null }>) => res.data,
      invalidatesTags: ['Forms', 'Subscribers', 'MktDashboard'],
    }),
    getCoupons: builder.query<CouponDto[], void>({
      query: () => ({ url: '/marketing/coupons' }),
      transformResponse: (res: ApiResponse<CouponDto[]>) => res.data,
      providesTags: ['Coupons'],
    }),
    createCoupon: builder.mutation<CouponDto, { code: string; discountPercent: number }>({
      query: (data) => ({ url: '/marketing/coupons', method: 'POST', data }),
      transformResponse: (res: ApiResponse<CouponDto>) => res.data,
      invalidatesTags: ['Coupons', 'MktDashboard'],
    }),

    // Support
    getSupportDashboard: builder.query<SupportDashboardDto, void>({
      query: () => ({ url: '/support/dashboard' }),
      transformResponse: (res: ApiResponse<SupportDashboardDto>) => res.data,
      providesTags: ['SupDashboard'],
    }),
    getSlaPolicies: builder.query<SlaPolicyDto[], void>({
      query: () => ({ url: '/support/slas' }),
      transformResponse: (res: ApiResponse<SlaPolicyDto[]>) => res.data,
    }),
    getTickets: builder.query<ApiResponse<TicketDto[]>, { status?: string; q?: string } | void>({
      query: (params) => ({ url: '/support/tickets', params: params || { page: 0, size: 50 } }),
      providesTags: ['Tickets'],
    }),
    createTicket: builder.mutation<
      TicketDto,
      {
        subject: string
        description: string
        channel: string
        priority: string
        requesterName: string
        requesterEmail: string
      }
    >({
      query: (data) => ({ url: '/support/tickets', method: 'POST', data }),
      transformResponse: (res: ApiResponse<TicketDto>) => res.data,
      invalidatesTags: ['Tickets', 'SupDashboard'],
    }),
    updateTicketStatus: builder.mutation<TicketDto, { id: string; status: string; escalate: boolean }>({
      query: ({ id, ...data }) => ({ url: `/support/tickets/${id}/status`, method: 'PATCH', data }),
      transformResponse: (res: ApiResponse<TicketDto>) => res.data,
      invalidatesTags: ['Tickets', 'SupDashboard'],
    }),
    getTicketComments: builder.query<TicketCommentDto[], string>({
      query: (ticketId) => ({ url: `/support/tickets/${ticketId}/comments` }),
      transformResponse: (res: ApiResponse<TicketCommentDto[]>) => res.data,
      providesTags: ['Comments'],
    }),
    addTicketComment: builder.mutation<TicketCommentDto, { ticketId: string; body: string; internalNote: boolean }>({
      query: ({ ticketId, ...data }) => ({ url: `/support/tickets/${ticketId}/comments`, method: 'POST', data }),
      transformResponse: (res: ApiResponse<TicketCommentDto>) => res.data,
      invalidatesTags: ['Comments', 'Tickets'],
    }),
    submitCsat: builder.mutation<TicketDto, { ticketId: string; rating: number; feedback?: string }>({
      query: ({ ticketId, ...data }) => ({ url: `/support/tickets/${ticketId}/csat`, method: 'POST', data }),
      transformResponse: (res: ApiResponse<TicketDto>) => res.data,
      invalidatesTags: ['Tickets', 'SupDashboard'],
    }),
    getKbArticles: builder.query<ApiResponse<KbArticleDto[]>, { q?: string } | void>({
      query: (params) => ({ url: '/support/kb', params: params || { page: 0, size: 50 } }),
      providesTags: ['KbArticles'],
    }),
    createKbArticle: builder.mutation<
      KbArticleDto,
      { title: string; category: string; content: string; published: boolean }
    >({
      query: (data) => ({ url: '/support/kb', method: 'POST', data }),
      transformResponse: (res: ApiResponse<KbArticleDto>) => res.data,
      invalidatesTags: ['KbArticles', 'SupDashboard'],
    }),
    voteKbHelpful: builder.mutation<KbArticleDto, string>({
      query: (id) => ({ url: `/support/kb/${id}/helpful`, method: 'POST' }),
      transformResponse: (res: ApiResponse<KbArticleDto>) => res.data,
      invalidatesTags: ['KbArticles'],
    }),
    getAppointments: builder.query<AppointmentDto[], void>({
      query: () => ({ url: '/support/appointments' }),
      transformResponse: (res: ApiResponse<AppointmentDto[]>) => res.data,
      providesTags: ['Appointments'],
    }),
    createAppointment: builder.mutation<
      AppointmentDto,
      { ticketId?: string; customerName: string; serviceAddress: string }
    >({
      query: (data) => ({ url: '/support/appointments', method: 'POST', data }),
      transformResponse: (res: ApiResponse<AppointmentDto>) => res.data,
      invalidatesTags: ['Appointments', 'SupDashboard'],
    }),
  }),
})

export const {
  useGetMarketingDashboardQuery,
  useGetSubscribersQuery,
  useUpsertSubscriberMutation,
  useGetSegmentsQuery,
  useCreateSegmentMutation,
  useGetCampaignsQuery,
  useCreateCampaignMutation,
  useLaunchCampaignMutation,
  useGetJourneysQuery,
  useCreateJourneyMutation,
  useGetFormsQuery,
  useCreateFormMutation,
  useSubmitMarketingFormMutation,
  useGetCouponsQuery,
  useCreateCouponMutation,
  useGetSupportDashboardQuery,
  useGetSlaPoliciesQuery,
  useGetTicketsQuery,
  useCreateTicketMutation,
  useUpdateTicketStatusMutation,
  useGetTicketCommentsQuery,
  useAddTicketCommentMutation,
  useSubmitCsatMutation,
  useGetKbArticlesQuery,
  useCreateKbArticleMutation,
  useVoteKbHelpfulMutation,
  useGetAppointmentsQuery,
  useCreateAppointmentMutation,
} = growthAndSupportApi

