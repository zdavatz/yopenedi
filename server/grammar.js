Grammar = [{
    "name": "UNA",
    "tag": "UNA",
    "render": "<UNA>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": "",
    isSkip: ""
}, {
    "name": "UNB",
    // "tag": "UNB",
    "tag": "",
    start: "",
    closeTAG: "",
    "render": "<UNB>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": "",
    isSkip: ""
}, {
    "name": "UNH",
    // "tag": "ORDER",
    "tag": "",
    // closeTag: "UNT",
    start: "",
    closeTAG: "",
    "render": "<UNH>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": "",

}, {
    "name": "BGM",
    tag: "ORDER_ID",
    renderxx: `$id`,
    render: `
    <ORDER_ID>$code-$id</ORDER_ID>`,
    "match": [
        ["", "$code", "$id"],
        []
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "DTM",
    "tag": "DTM",
    "render": `
    <DELIVERY_DATE type="optional">
        <DELIVERY_START_DATE>$DTM</DELIVERY_START_DATE>
        <DELIVERY_END_DATE>$FORMAT</DELIVERY_END_DATE>
    </DELIVERY_DATE>`,
    "match": [
        [],
        ["$CODE", "$DTM", "$FORMAT"]
    ],
    cases: ["$CODE"],
    11: ``,
    17: ``,
    59: ``,
    64: ``,
    "X13": ``,
    2: `
    <DELIVERY_DATE type="optional">
    <DELIVERY_START_DATE>$DTM</DELIVERY_START_DATE></DELIVERY_DATE>`,
    137: `<ORDER_DATE>$DTM</ORDER_DATE>`,
    162: ``,
    191: ``,
    162: ``,
    191: ``,
    200: ``,
    234: ``,
    235: ``,
    359: ``,
    "54E": ``,
    exc: function (id) {
        var line = this[id]
        return line
    },
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "FTX",
    "tag": "FreeText",
    renderX:'',
    "render": "<FreeText><CODE>$code</CODE><CONTENT>$FREETXT1 - $FREETXT2</CONTENT></FreeText>",
    "match": [
        ["", "$code", "$REF"],
        ["$FREETXT1", "$FREETXT2"]
    ],
 
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "RFF",
    "tag": "REFERENCE",
    renderx:'',
    "render": "<REFERENCE> $value  - $extra</REFERENCE>",
    "match": [
        ["", "$value", "$REF"],
        ["$code", "$extra"]
    ],
    "parent": "",
    "children": "",
    "isHeader": "",
    
    AJK: "<HEADER_UDX><UDX.JA.DeliveryConditionCode>$code</UDX.JA.DeliveryConditionCode><UDX.JA.DeliveryConditionDetails>$extra</UDX.JA.DeliveryConditionDetails></HEADER_UDX>",
    //<REFERENCETTTT> $value  - $extra</REFERENCETTTT>
    // isSkipped: true,
    cases: ["$code"],
    isLineRendered : true,
    exc: function (id) {
        if(this[id]){
            var line = this[id]
            return line
        }

    },
    lineRender: function(id){

    }
}, {
    "name": "NAD",
    "tag": "",
    renderxx: `
    <bmecat:PARTY_ID>$ID</bmecat:PARTY_ID>
    <PARTY_ROLE>$ROLE</PARTY_ROLE>
    <bmecat:PARTY_ID type="supplier_specific">$CODE</bmecat:PARTY_ID>`,
    render: `
    <PARTY>
    <PARTY_ID type="iln">$ID</PARTY_ID>
    <PARTY_ROLE>$ROLE</PARTY_ROLE>
    </PARTY>`,
    match: [
        ["", "$ROLE"],
        ["$ID", "", "$CODE"]
    ],
    SU: `<bmecat:PARTY_ID type="iln">$ID</bmecat:PARTY_ID>
    <PARTY_ROLE>supplier</PARTY_ROLE>`,
    BY: `<bmecat:PARTY_ID type="iln">$ID</bmecat:PARTY_ID>
    <bmecat:PARTY_ID type="supplier_specific">$CODE</bmecat:PARTY_ID>
    <PARTY_ROLE>buyer</PARTY_ROLE>`,
    DP: `<bmecat:PARTY_ID type="iln">$ID</bmecat:PARTY_ID>
    <bmecat:PARTY_ID type="supplier_specific">CODE</bmecat:PARTY_ID>
    <PARTY_ROLE>Depo</PARTY_ROLE>`,
    exc: function (id) {
        var line = this[id]
        return line
    },
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "CTA",
    tag: "COMMUNICATION_INFORMATION",
    "renderOLD": `
    <COMMUNICATION_INFORMATION>
    <FUNCTION_CODE>$FUNCTIONCODE</FUNCTION_CODE>
    <DEPCODE>$DEP</DEPCODE>
    <DEPCODE_NAME>$CODENAME</DEPCODE_NAME>
    <PERSON>$PERSONDEP</PERSON>
    </COMMUNICATION_INFORMATION>`,
    render: ``,
    match: [ 
        ["", "$FUNCTIONCODE", "$DEP"],
        ["$CODENAME", "$PERSONDEP", "$type"]
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "COM",
    "tag": "COMMUNICATION",
    "render": `
    <COMMUNICATION>
        <TYPE>$type</TYPE>
        <CONTACT>$CONTACT</CONTACT>
    </COMMUNICATION>`,
    match: [
        [],
        ["$CONTACT", "$type"]
    ],
    cases: ["$type"],
    "EM": `<bmecat:EMAILS>$CONTACT</bmecat:EMAILS>`,
    "TE": `<bmecat:PHONE>$CONTACT</bmecat:PHONE>`,
    exc: function (id) {
        var line = this[id]
        return line
    },
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "CUX",
    tag: "",
    render: '<CURRENCY>$id</CURRENCY>',
    renderx: '<bmecat:CURRENCY>$id</bmecat:CURRENCY>',
    match: [
        [],
        ["$role", "$id", "$ex"]
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "LIN",
    "tag": "ORDER_ITEM",
    render: '<LINE_ITEM_ID>$id</LINE_ITEM_ID><bmecat:INTERNATIONAL_PID type="ean">$productID</bmecat:INTERNATIONAL_PID>',
    "renderx": `<LINE_ITEM_ID>$id</LINE_ITEM_ID><PRODUCT_CUSTOM_ID>$productID</PRODUCT_CUSTOM_ID><PRODUCT_LANG>$lang</PRODUCT_LANG>`,
    match: [
        ["", "$id"],
        ["$productID", "$lang"]
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "PIA",
    desc: "ITEM ID",
    "tag": "PIA",
    render:'',
    "renderOLD": `
    <PRODUCT_ID>
    <CODE>$code</CODE>
    <ID>$ID_CODE</ID>
    <ITEM_NUMBER_ID>$ITEM_NUMBER_IDENTIFICATION</ITEM_NUMBER_ID>
    </PRODUCT_ID>`,
    "match": [
        ["", "$code"],
        ["$ID", "$ID_CODE", "", "$ITEM_NUMBER_IDENTIFICATION"]
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "IMD",
    "tag": "ITEM_DESCRIPTION",
    // render: '<ITEM_DESCRIPTION>$CODE - $ID</ITEM_DESCRIPTION>',
    render: '',
    match: [
        ["", "$CODE"],
        ["", "", "", "$ID"]
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "QTY",
    "tag": "QTY",
    renderx: '<QUANTITY>$qty</QUANTITY><ORDER_UNIT>$unit</ORDER_UNIT>',
    renderold: '<QUANTITY>$qty</QUANTITY><bmecat:ORDER_UNIT>$unit</bmecat:ORDER_UNIT>',
    render: '<bmecat:ORDER_UNIT>$unit</bmecat:ORDER_UNIT><QUANTITY>$qty</QUANTITY>',
    match: [
        [''],
        ['$code', '$qty', '$unit']
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "PRI",
    "tag": "PRI",
    renderx: `
    <PRODUCT_PRICE_FIX>
        <PRICE_AMOUNT>$PRICE</PRICE_AMOUNT>
        <PRICE_QUANTITY>$PRICEQUALIFIER</PRICE_QUANTITY>
        <PRICE_UNIT>$UNIT</PRICE_UNIT>
    </PRODUCT_PRICE_FIX>
    `,    
    renderX: `
    <PRODUCT_PRICE_FIX>
        <bmecat:PRICE_AMOUNT>$PRICE</bmecat:PRICE_AMOUNT>
        <bmecat:PRICE_QUANTITY>$PRICEQUALIFIER</bmecat:PRICE_QUANTITY>
        <PRICE_UNIT>$UNIT</PRICE_UNIT>
    </PRODUCT_PRICE_FIX>
    `,
    render: `
    <PRODUCT_PRICE_FIX>
    <bmecat:PRICE_AMOUNT>$UNIT</bmecat:PRICE_AMOUNT>
    <bmecat:PRICE_QUANTITY>$PRICE</bmecat:PRICE_QUANTITY>
    <ALLOW_OR_CHARGES_FIX>
    </ALLOW_OR_CHARGES_FIX>
    <TAX_DETAILS>
        <TAX_TYPE>VAT</TAX_TYPE>
        <TAX>0</TAX>
        <TAX_CATEGORY>exemption</TAX_CATEGORY>
    </TAX_DETAILS>
    </PRODUCT_PRICE_FIX>`,
    "match": [
        [],
        ["$PRICEQUALIFIER", "$PRICE", "", "", "$UNIT"]
    ],
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "UNS",
    "tag": "UNS",
    "render": "",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "UNT",
    "tag": "UNT",
    "render": "</UNH>",
    closeTAG:  "</UNH>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": ""
}, {
    "name": "UNZ",
    "tag": "UNZ",
    "render": "</UNB>",
    closeTAG: "</UNB>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": ""
},
{
    "name": "PRODUCTS",
    "tag": "ORDER_ITEM_LIST",
    "closeTAG": "PRODUCTS",
    // render: "</PRODUCTS>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": ""
},
{
    "name": "PARTIES",
    "tag": "PARTIES",
    "closeTAG": "PARTIES",
    // render: "</PARTIES>",
    "match": "",
    "parent": "",
    "children": "",
    "isHeader": ""
},{
    name: "ORDER_HEADER ",
    tag: "ORDER_HEADER "
},{
    name: "CONTROL_INFO ",
    tag: "CONTROL_INFO "
}
]