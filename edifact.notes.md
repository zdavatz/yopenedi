
http://www.danga.biz/2008/06/short-description-of-unedifact-message.html


- Segment terminator = apostrophe '

- Segment tag and data element separator = plus sign +

- Component data element separator = colon :



<!-- ----------------------------------------------------------------------- -->


Segment groups are used to aggregate several individual segments into groups of related segments.

For example, the following segment group allows to specify contact details by combining the segments CTA (Contact information) and COM (Communication contact).

  0220       ----- Segment group 5  ------------------ C   5----------+|
  0230   CTA Contact information                       M   1          ||
  0240   COM Communication contact                     C   5----------++
Some possible segment sequences would be for example:

 CTA-CTA-CTA-COM-COM-CTA-COM
 CTA
 CTA-COM-CTA-CTA
 ...
As indicated by C 5, the segment group itself is optional and may occur up to 5 times. The segment group is initiated by a so-called trigger segment. It is the first element within the segment group that usually has cardinality M 1 (that is, it must occur exactly once).

<!-- ----------------------------------------------------------------------- -->

DTM segment
DTM+137:20180218:102'
DTM+2:20180220:102'
The DTM segment is used to specify date and time information.

The first part of this composite data element identifies the type of the date (date/time/period qualifier). For example:

137 = Document/message date/time. Date/time when a document/message has been issued.
2 = Delivery date/time, requested date on which buyer requests goods to be delivered

The second part represents the actual date value:

20180218 for example, February 18, 2018.

The third part specifies the pattern for the date (date/time/period format qualifier).

102 corresponds to CCYYMMDD


<!-- ----------------------------------------------------------------------- -->


UNA
UNB -<Exchange Header>

UNG - Function Header

<!-- ----------------------------------------------------------------------- -->

UNH - Message Header
UNT - Message Tail

UNT - <Exchange Tail>

<!-- ----------------------------------------------------------------------- -->

BGM - MSG start

DTM - DateTime


get segment head:
    -> seperate segment and data element 
    --> check if the rest has 


<!-- ----------------------------------------------------------------------- -->



UNB -> ORDER
	UNH -> ORDER_HEADER
		ORDERS []
			BGM -> Order info
			DTM
			DTM
			FTX
			REF +
			