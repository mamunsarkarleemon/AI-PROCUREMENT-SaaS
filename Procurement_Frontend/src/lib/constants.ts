import type { OrderSource, OrderStatus } from '../api/types'

export const UNITS = ['pcs', 'piece', 'box', 'carton', 'pack', 'set', 'kg', 'ton', 'litre', 'bag', 'roll', 'sqm', 'metre']

export const ORDER_STEPS: OrderStatus[] = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED']

/** Button text for each order status change. */
export const ORDER_ACTION_LABEL: Record<OrderStatus, string> = {
  PENDING: 'Set back to pending',
  CONFIRMED: 'Confirm order',
  SHIPPED: 'Mark as shipped',
  DELIVERED: 'Mark as received',
  CANCELLED: 'Cancel order',
}

export const ORDER_SOURCE_LABEL: Record<OrderSource, string> = {
  DIRECT: 'Buy now',
  NEGOTIATION: 'Negotiated',
  QUOTE: 'From quote',
}
