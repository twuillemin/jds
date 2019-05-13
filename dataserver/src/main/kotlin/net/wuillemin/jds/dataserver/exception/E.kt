package net.wuillemin.jds.dataserver.exception

import net.wuillemin.jds.common.exception.ExceptionCode

@Suppress("KDocMissingDocumentation", "PublicApiImplicitType")
object E {

    val controller = Controller()
    val service = Service()
    val supplier = Supplier()

    class Controller {

        val dataAccess = DataAccess()
        val dataProvider = DataProvider()
        val dataSource = DataSource()
        val schema = Schema()
        val server = Server()

        class DataAccess {
            val getDataDenied = ExceptionCode("dataserver.controller.dataaccess.get_data_denied")
            val getColumnsDenied = ExceptionCode("dataserver.controller.dataaccess.get_columns_denied")
            val insertDenied = ExceptionCode("dataserver.controller.dataaccess.insert_denied")
            val massInsertDenied = ExceptionCode("dataserver.controller.dataaccess.mass_insert_denied")
            val updateDenied = ExceptionCode("dataserver.controller.dataaccess.update_denied")
            val deleteDenied = ExceptionCode("dataserver.controller.dataaccess.delete_denied")
        }

        class DataProvider {
            val createDenied = ExceptionCode("dataserver.controller.dataprovider.create_denied")
            val deleteDenied = ExceptionCode("dataserver.controller.dataprovider.delete_denied")
            val getDenied = ExceptionCode("dataserver.controller.dataprovider.get_denied")
            val previewDenied = ExceptionCode("dataserver.controller.dataprovider.preview_denied")
            val getDataSourcesDenied = ExceptionCode("dataserver.controller.dataprovider.get_datasources_denied")
            val importSqlDenied = ExceptionCode("dataserver.controller.dataprovider.import_sql_denied")
            val updateDenied = ExceptionCode("dataserver.controller.dataprovider.update_denied")
            val updateDifferentIds = ExceptionCode("dataserver.controller.dataprovider.update_different_ids")
        }

        class DataSource {
            val createDenied = ExceptionCode("dataserver.controller.datasource.create_denied")
            val deleteDenied = ExceptionCode("dataserver.controller.datasource.delete_denied")
            val getDenied = ExceptionCode("dataserver.controller.datasource.get_denied")
            val updateDenied = ExceptionCode("dataserver.controller.datasource.update_denied")
            val updateDifferentIds = ExceptionCode("dataserver.controller.datasource.update_different_ids")
        }

        class Schema {
            val createDenied = ExceptionCode("dataserver.controller.schema.create_denied")
            val createInternalDenied = ExceptionCode("dataserver.controller.schema.create_internal_denied")
            val deleteDenied = ExceptionCode("dataserver.controller.schema.delete_denied")
            val deleteInternalDenied = ExceptionCode("dataserver.controller.schema.delete_internal_denied")
            val getDataProvidersDenied = ExceptionCode("dataserver.controller.schema.get_dataproviders_denied")
            val getDenied = ExceptionCode("dataserver.controller.schema.get_denied")
            val previewTableDenied = ExceptionCode("dataserver.controller.schema.preview_table_denied")
            val getTablesDenied = ExceptionCode("dataserver.controller.schema.get_tables_denied")
            val updateDenied = ExceptionCode("dataserver.controller.schema.update_denied")
            val updateInternalDenied = ExceptionCode("dataserver.controller.schema.update_internal_denied")
            val updateDifferentIds = ExceptionCode("dataserver.controller.schema.update_different_ids")
        }

        class Server {
            val createDenied = ExceptionCode("dataserver.controller.server.create_denied")
            val createInternalDenied = ExceptionCode("dataserver.controller.server.create_internal_denied")
            val deleteDenied = ExceptionCode("dataserver.controller.server.delete_denied")
            val deleteInternalDenied = ExceptionCode("dataserver.controller.server.delete_internal_denied")
            val getSchemasDenied = ExceptionCode("dataserver.controller.server.get_schemas_denied")
            val getDenied = ExceptionCode("dataserver.controller.server.get_denied")
            val updateDenied = ExceptionCode("dataserver.controller.server.update_denied")
            val updateInternalDenied = ExceptionCode("dataserver.controller.server.update_internal_denied")
            val updateDifferentIds = ExceptionCode("dataserver.controller.server.update_different_ids")
        }
    }

    class Service {

        val access = Access()
        val import = Import()
        val model = Model()
        val query = Query()

        class Access {
            val dataProviderNotSupported = ExceptionCode("dataserver.service.access.data_provider_not_supported")
            val schemaNotSupported = ExceptionCode("dataserver.service.access.schema_not_supported")
        }

        class Import {
            val groupNotPersisted = ExceptionCode("dataserver.service.import.initialize.group_not_persisted")
            val dataProviderNotSupported = ExceptionCode("dataserver.service.import.import.data_provider_not_supported")
        }

        class Model {

            val dataProvider = DataProvider()
            val dataSource = DataSource()
            val lookup = Lookup()
            val preview = Preview()
            val schema = Schema()
            val server = Server()

            class DataProvider {
                val getForSchemaNotPersisted = ExceptionCode("dataserver.service.model.dataprovider.get_for_schema_not_persisted")
                val getForSchemasNotPersisted = ExceptionCode("dataserver.service.model.dataprovider.get_for_schemas_not_persisted")
                val createAlreadyPersisted = ExceptionCode("dataserver.service.model.dataprovider.create_already_persisted")
                val updateNotPersisted = ExceptionCode("dataserver.service.model.dataprovider.update_not_persisted")
                val updateSchemaId = ExceptionCode("dataserver.service.model.dataprovider.update_schemaid")
                val deleteReferenced = ExceptionCode("dataserver.service.model.dataprovider.delete_referenced")
                val deleteNotPersisted = ExceptionCode("dataserver.service.model.dataprovider.delete_not_persisted")
            }

            class DataSource {
                val getForDataProviderNoId = ExceptionCode("dataserver.service.model.datasource.get_for_dataprovider_not_persisted")
                val getForDataProvidersNoId = ExceptionCode("dataserver.service.model.datasource.get_for_dataproviders_not_persisted")
                val createAlreadyPersisted = ExceptionCode("dataserver.service.model.datasource.create_already_persisted")
                val updateNotPersisted = ExceptionCode("dataserver.service.model.datasource.update_not_persisted")
                val updateDataProviderId = ExceptionCode("dataserver.service.model.datasource.update_dataproviderid")
                val deleteNotPersisted = ExceptionCode("dataserver.service.model.datasource.delete_not_persisted")
            }

            class Lookup {
                val columnDoesNotExist = ExceptionCode("dataserver.service.model.lookup.promote_column_not_exist")
                val columnIsNotString = ExceptionCode("dataserver.service.model.lookup.promote_column_is_not_string")
                val dataProviderWithoutPrimaryKey = ExceptionCode("dataserver.service.model.lookup.promote_dataprovider_without_primary_key")
                val tooManyValues = ExceptionCode("dataserver.service.model.lookup.promote_too_many_values")
                val unknownLookupValue = ExceptionCode("dataserver.service.model.lookup.promote_unknown_lookup_value")
                val tooLargeLookup = ExceptionCode("dataserver.service.model.lookup.promote_too_large_lookup")
                val failedToPromoteLookup = ExceptionCode("dataserver.service.model.lookup.promote_failed_to_promote_lookup")
                val tooManyLookupsForColumn = ExceptionCode("dataserver.service.model.lookup.validate_too_many_lookups_for_column")
                val badLookupForColumn = ExceptionCode("dataserver.service.model.lookup.validate_bad_lookup_for_column")
                val badValueDataTypeForColumn = ExceptionCode("dataserver.service.model.lookup.validate_bad_data_type_for_column")
            }

            class Preview {
                val schemaNotSupported = ExceptionCode("dataserver.service.model.preview.schema_not_supported")
                val dataProviderNotSupported = ExceptionCode("dataserver.service.model.preview.data_provider_not_supported")
            }

            class Schema {
                val getForServerNoId = ExceptionCode("dataserver.service.model.schema.get_for_server_not_persisted")
                val getForServersNoId = ExceptionCode("dataserver.service.model.schema.get_for_servers_not_persisted")
                val createAlreadyPersisted = ExceptionCode("dataserver.service.model.schema.create_already_persisted")
                val updateNotPersisted = ExceptionCode("dataserver.service.model.schema.update_not_persisted")
                val updateServerId = ExceptionCode("dataserver.service.model.schema.update_serverid")
                val deleteReferenced = ExceptionCode("dataserver.service.model.schema.delete_referenced")
                val deleteNotPersisted = ExceptionCode("dataserver.service.model.schema.delete_not_persisted")
            }

            class Server {
                val createAlreadyPersisted = ExceptionCode("dataserver.service.model.server.create_already_persisted")
                val updateNotPersisted = ExceptionCode("dataserver.service.model.server.update_not_persisted")
                val updateCustomerDefined = ExceptionCode("dataserver.service.model.server.update_customerdefined")
                val updateGroupId = ExceptionCode("dataserver.service.model.server.update_groupid")
                val deleteReferenced = ExceptionCode("dataserver.service.model.server.delete_referenced")
                val deleteNotPersisted = ExceptionCode("dataserver.service.model.server.delete_not_persisted")
            }
        }

        class Query {
            val andChildrenNotBoolean = ExceptionCode("dataserver.service.query.and_children_not_boolean")
            val likeColumnNotString = ExceptionCode("dataserver.service.query.like_column_not_string")
            val likeValueNotString = ExceptionCode("dataserver.service.query.like_value_not_string")
            val orChildrenNotBoolean = ExceptionCode("dataserver.service.query.or_children_not_boolean")
            val differentLeftAndRightType = ExceptionCode("dataserver.service.query.predicate_different_left_right")
            val columnNotInDataProvider = ExceptionCode("dataserver.service.query.column_not_in_dataspec")
            val unableToWalkElement = ExceptionCode("dataserver.service.query.unable_to_walk")
        }
    }

    class Supplier {

        val sql = SQL()

        class SQL {

            val service = Service()
            val util = Util()

            class Service {
                val serverIsNotSql = ExceptionCode("dataserver.supplier.sql.service.server_is_not_sql")
                val schemaIsNotSql = ExceptionCode("dataserver.supplier.sql.service.schema_is_not_sql")
                val dataProviderNotEditable = ExceptionCode("dataserver.supplier.sql.service.dataprovider_not_editable")
                val cacheSchemaNotPersisted = ExceptionCode("dataserver.supplier.sql.service.cache_schema_not_persisted")
                val attributeDoesNotExist = ExceptionCode("dataserver.supplier.sql.service.insert_attribute_not_exist")
                val importerServerNotPersisted = ExceptionCode("dataserver.supplier.sql.service.importer_server_not_persisted")
                val importerDuplicatedColumns = ExceptionCode("dataserver.supplier.sql.service.importer_duplicated_columns")
                val lookupMustBeProvidedAsList = ExceptionCode("dataserver.supplier.sql.service.lookup_must_be_provided_as_list")
            }

            class Util {
                val jdbcTypeNotSupported = ExceptionCode("dataserver.supplier.sql.util.jdbc_type_not_supported")
                val jdbcColumnTypeNotSupported = ExceptionCode("dataserver.supplier.sql.util.jdbc_column_type_not_supported")
                val sqlParseError = ExceptionCode("dataserver.supplier.sql.util.sql_parse_error")
                val sqlParseSelectError = ExceptionCode("dataserver.supplier.sql.util.sql_parse_select_error")
                val orderColumn = ExceptionCode("dataserver.supplier.sql.util.order_column")
                val converterBadRequestElement = ExceptionCode("dataserver.supplier.sql.util.converter_bad_request_element")
                val converterBadPredicate = ExceptionCode("dataserver.supplier.sql.util.converter_bad_predicate")
                val converterLikeNotOnString = ExceptionCode("dataserver.supplier.sql.util.converter_like_not_on_string")
            }
        }
    }
}