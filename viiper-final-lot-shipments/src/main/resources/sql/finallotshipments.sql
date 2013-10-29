SELECT DISTINCT wdd.lot_number AS lot_number
FROM apps.wsh_delivery_details wdd
  , apps.wsh_new_deliveries  wnd  
  , apps.wsh_delivery_assignments wda
  , apps.hr_organization_units ou
  , apps.mtl_system_items msi
WHERE wda.delivery_detail_id     = wdd.delivery_detail_id
AND wda.delivery_id              = wnd.delivery_id
AND ou.organization_id           = wdd.organization_id
AND msi.inventory_item_id        = wdd.inventory_item_id
AND msi.organization_id          = wdd.organization_id
AND msi.item_type                = 'FG'
AND wdd.source_header_type_name != 'Internal Order'
AND wdd.lot_number               = '${in.header.LOTNAME}'
AND wdd.transaction_id           = ${in.header.TRANSACTION_ID}
AND NOT EXISTS
    (SELECT 1
        FROM apps.mtl_onhand_quantities moq
        WHERE wdd.lot_number           = moq.lot_number
        AND moq.subinventory_code NOT IN ('SHIP STAGE','SAMPLE FG')
    ) 
ORDER BY lot_number