This is a tool for the game Satisfactory allowing you to organize and manage your factory.

## Save parser

`antafes.sf.saveparser.SaveParser` is a stateless Spring `@Service` for read-only
Satisfactory save parsing. Inject it into Spring-managed consumers and call
`readSaveFileInfo(Path)` for metadata or `readFullSaveFile(Path)` for the complete
world structure. It supports the legacy header 10/save version 36 format used by the
included regression fixture, plus current header 14 save versions 52, 53, and 58-60.
The legacy format exposes its level and object-header structure; its older object payload
layouts are intentionally not interpreted as modern properties. The parser does not modify
or write saves.
