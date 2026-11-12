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

export interface AgentDescriptorDto {
  code: string
  name: string
  description: string
  requiredPermission: string
}

export interface KnowledgeDocDto {
  id: string
  title: string
  sourceType: string
  chunkCount: number
  createdAt: string
}

export interface RagCitationDto {
  documentId: string
  documentTitle: string
  chunkIndex: number
  snippet: string
  score: number
}

export interface AiConversationDto {
  id: string
  title: string
  agentCode: string
  updatedAt: string
}

export interface AiMessageDto {
  id: string
  role: string
  content: string
  citations: RagCitationDto[]
  createdAt: string
}

export interface AiConversationDetailDto {
  conversation: AiConversationDto
  messages: AiMessageDto[]
}

export interface AiActionProposalDto {
  id: string
  conversationId: string | null
  agentCode: string
  actionType: string
  summary: string
  status: string
  approvedBy: string | null
  executedAt: string | null
  createdAt: string
}

export interface AiChatResponseDto {
  conversationId: string
  agentCode: string
  reply: AiMessageDto
  citations: RagCitationDto[]
  proposal: AiActionProposalDto | null
}

export interface AiDomainInsightDto {
  domain: string
  title: string
  summary: string
  severity: string
}

export const aiApi = createApi({
  reducerPath: 'aiApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['AiAgents', 'AiKnowledge', 'AiConversations', 'AiProposals', 'AiInsights'],
  endpoints: (builder) => ({
    getAiAgents: builder.query<AgentDescriptorDto[], void>({
      query: () => ({ url: '/ai/agents' }),
      transformResponse: (res: ApiResponse<AgentDescriptorDto[]>) => res.data,
      providesTags: ['AiAgents'],
    }),
    getAiInsights: builder.query<AiDomainInsightDto[], void>({
      query: () => ({ url: '/ai/insights' }),
      transformResponse: (res: ApiResponse<AiDomainInsightDto[]>) => res.data,
      providesTags: ['AiInsights'],
    }),
    getKnowledgeDocs: builder.query<KnowledgeDocDto[], void>({
      query: () => ({ url: '/ai/knowledge' }),
      transformResponse: (res: ApiResponse<KnowledgeDocDto[]>) => res.data,
      providesTags: ['AiKnowledge'],
    }),
    ingestKnowledge: builder.mutation<KnowledgeDocDto, { title: string; sourceType?: string; contentText: string }>({
      query: (data) => ({ url: '/ai/knowledge', method: 'POST', data }),
      transformResponse: (res: ApiResponse<KnowledgeDocDto>) => res.data,
      invalidatesTags: ['AiKnowledge'],
    }),
    searchKnowledge: builder.mutation<RagCitationDto[], { query: string; topK?: number }>({
      query: (data) => ({ url: '/ai/knowledge/search', method: 'POST', data }),
      transformResponse: (res: ApiResponse<RagCitationDto[]>) => res.data,
    }),
    getConversations: builder.query<AiConversationDto[], void>({
      query: () => ({ url: '/ai/conversations' }),
      transformResponse: (res: ApiResponse<AiConversationDto[]>) => res.data,
      providesTags: ['AiConversations'],
    }),
    getConversationDetail: builder.query<AiConversationDetailDto, string>({
      query: (id) => ({ url: `/ai/conversations/${id}` }),
      transformResponse: (res: ApiResponse<AiConversationDetailDto>) => res.data,
      providesTags: ['AiConversations'],
    }),
    sendAiChat: builder.mutation<AiChatResponseDto, { conversationId?: string; agentCode?: string; message: string }>({
      query: (data) => ({ url: '/ai/chat', method: 'POST', data }),
      transformResponse: (res: ApiResponse<AiChatResponseDto>) => res.data,
      invalidatesTags: ['AiConversations', 'AiProposals'],
    }),
    getProposals: builder.query<AiActionProposalDto[], void>({
      query: () => ({ url: '/ai/proposals' }),
      transformResponse: (res: ApiResponse<AiActionProposalDto[]>) => res.data,
      providesTags: ['AiProposals'],
    }),
    approveProposal: builder.mutation<AiActionProposalDto, string>({
      query: (id) => ({ url: `/ai/proposals/${id}/approve`, method: 'POST' }),
      transformResponse: (res: ApiResponse<AiActionProposalDto>) => res.data,
      invalidatesTags: ['AiProposals'],
    }),
    rejectProposal: builder.mutation<AiActionProposalDto, string>({
      query: (id) => ({ url: `/ai/proposals/${id}/reject`, method: 'POST' }),
      transformResponse: (res: ApiResponse<AiActionProposalDto>) => res.data,
      invalidatesTags: ['AiProposals'],
    }),
  }),
})

export const {
  useGetAiAgentsQuery,
  useGetAiInsightsQuery,
  useGetKnowledgeDocsQuery,
  useIngestKnowledgeMutation,
  useSearchKnowledgeMutation,
  useGetConversationsQuery,
  useGetConversationDetailQuery,
  useSendAiChatMutation,
  useGetProposalsQuery,
  useApproveProposalMutation,
  useRejectProposalMutation,
} = aiApi

