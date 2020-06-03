

Grammar = [{
    name: "LIN",
    tag: "PRODUCT",
},
{
    name: "BGM",
    tag: "Message",
    segments: ["TYPE", "VALUE"],
    desc: "",
    render: '<BGM>$id</BGM>',
    match: ["$code", "$id"],
},
{
    name: "NAD",
    tag: "PARTY",
    segments: ["TYPE", "VALUE"],
    desc: "",
    render: '<PARTY><PARTY_ID>$id</PARTY_ID><PARTY_ROLE>$role</PARTY_ROLE></PARTY>',
    match: ["$role", "$id", "$ex"],
},
{
    name: "QTY",
    tag: "QUANTITY",
    render: '<QUANTITY attr="$code">$qty</QUANTITY><ORDER_UNIT>$unit</ORDER_UNIT>',
    match: ["$code", "$qty", "$unit"]
},
{
    name: "DTM",
    tag: "DATE_TIME",
    render: '<DATE_TIME>$date</DATE_TIME>',
    match: ["$code", "$date"],
},
{
    name: "IMD",
    tag: "ITEM_DESCRIPTION",
    render: '<ITEM_DESCRIPTION>$item</ITEM_DESCRIPTION>',
    match: ["$code", "$id", "$agency", "$desc"],
},
{
    name: "RFF",
    tag: "REFERENCE"
},
{
    name: "CTA",
    tag: "COMMUNICATION_INFORMATION",
    desc: "Contact function code + NAME",
    parentTo: "COM"
},
{
    name: "COM",
    tag: "COMMUNICATION",
},
{
    name: "FTX",
    tag: "FTX",
    desc: "FreeText"
}, {
    name: "CPS",
    tag: "CPS",
    desc: "CPS"

},
{
    name: "PAC",
    tag: "PAC",
    desc: "The PAC segment is used to specify the number and the type of packages.",

},
{
    name: "PCI",
    tag: "PCI",
    desc: "",
},
{
    name: "GIN",
    tag: "GIN",
    desc: "",
},
{
    name: "CUX",
    tag: "CURRENCY",
    render: '<CURRENCY>$id</CURRENCY>',
    match: ["$usageCode", "$id", "$typeCode"]
}
]
