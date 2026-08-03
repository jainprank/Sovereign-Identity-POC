import type { AxiosInstance } from "axios";
import type { NotificationResponse } from "./types";

// Thin wrapper around com.idtrust.notification.NotificationController — self-service, no role
// restriction, so a HOLDER session reads exactly its own feed (see the controller's Javadoc).
// Recipients are every ACTIVE user of the tenant (NotificationEventPublisherImpl), so a holder
// sees tenant-wide events like CredentialIssued/VerificationCompleted, not only ones about their
// own wallet — a real, if broad, feed rather than a fabricated per-wallet one.

export async function listNotifications(api: AxiosInstance): Promise<NotificationResponse[]> {
  const { data } = await api.get<NotificationResponse[]>("/api/v1/notifications", { params: { size: 50 } });
  return data;
}

export async function unreadNotificationCount(api: AxiosInstance): Promise<number> {
  const { data } = await api.get<{ unread: number }>("/api/v1/notifications/unread-count");
  return data.unread;
}

export async function markNotificationRead(api: AxiosInstance, id: string): Promise<NotificationResponse> {
  const { data } = await api.post<NotificationResponse>(`/api/v1/notifications/${id}/read`);
  return data;
}
