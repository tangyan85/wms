<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
	<script>
		var stockin_repository = null;// 入库仓库编号
		var stockin_supplier = null;// 入库供应商编号
		var stockin_goods = null;// 入库货物编号
		var stockin_number = null;// 入库数量

		var supplierCache = new Array();// 供应商信息缓存
		var goodsCache = new Array();//货物信息缓存

		$(function () {
			repositorySelectorInit();
			dataValidateInit();
			detilInfoToggle();

			stockInOption();
			fetchStorage();
			supplierAutocomplete();
			goodsAutocomplete();
		})

		// 数据校验
		function dataValidateInit() {
			$('#stockin_form').bootstrapValidator({
				message: 'This is not valid',

				fields: {
					stockin_input: {
						validators: {
							notEmpty: {
								message: '入库数量不能为空'
							},
							greaterThan: {
								value: 0,
								message: '入库数量不能小于0'
							}
						}
					}
				}
			})
		}

		// 货物信息自动匹配
		function goodsAutocomplete() {
			$('#goods_input').autocomplete({
				minLength: 0,
				//delay: 100,
				source: function (request, response) {
					$.ajax({
						type: 'GET',
						url: 'goodsManage/getGoodsList',
						dataType: 'json',
						contentType: 'application/json',
						data: {
							offset: -1,
							limit: -1,
							keyWord: request.term,
							searchType: 'searchByName'
						},
						success: function (data) {
							var autoCompleteInfo = new Array();
							$.each(data.rows, function (index, elem) {
								goodsCache.push(elem);
								autoCompleteInfo.push({ label: elem.name, value: elem.id });
							});
							response(autoCompleteInfo);
						}
					});
				},
				focus: function (event, ui) {
					$('#goods_input').val(ui.item.label);
					return false;
				},
				select: function (event, ui) {
					$('#goods_input').val(ui.item.label);
					stockin_goods = ui.item.value;
					goodsInfoSet(stockin_goods);
					loadStorageInfo();
					return false;
				}
			})
		}

		// 供应商信息自动匹配
		function supplierAutocomplete() {
			$('#supplier_input').autocomplete({
				minLength: 0,
				//delay: 100,
				source: function (request, response) {
					$.ajax({
						type: "GET",
						url: 'supplierManage/getSupplierList',
						dataType: 'json',
						contentType: 'application/json',
						data: {
							offset: -1,
							limit: -1,
							searchType: 'searchByName',
							keyWord: request.term
						},
						success: function (data) {
							var autoCompleteInfo = new Array();
							$.each(data.rows, function (index, elem) {
								supplierCache.push(elem);
								autoCompleteInfo.push({ label: elem.name, value: elem.id });
							});
							response(autoCompleteInfo);
						}
					});
				},
				focus: function (event, ui) {
					$('#supplier_input').val(ui.item.label);
					return false;
				},
				select: function (event, ui) {
					$('#supplier_input').val(ui.item.label);
					stockin_supplier = ui.item.value;
					supplierInfoSet(stockin_supplier);
					return false;
				}
			})
		}

		// 填充供应商详细信息
		function supplierInfoSet(supplierID) {
			var detailInfo;
			$.each(supplierCache, function (index, elem) {
				if (elem.id == supplierID) {
					detailInfo = elem;

					if (detailInfo.id == null)
						$('#info_supplier_ID').text('-');
					else
						$('#info_supplier_ID').text(detailInfo.id);

					if (detailInfo.name == null)
						$('#info_supplier_name').text('-');
					else
						$('#info_supplier_name').text(detailInfo.name);

					if (detailInfo.tel == null)
						$('#info_supplier_tel').text('-');
					else
						$('#info_supplier_tel').text(detailInfo.tel);

					if (detailInfo.personInCharge == null)
						$('#info_supplier_person').text('-');
					else
						$('#info_supplier_person').text(detailInfo.personInCharge);

					if (detailInfo.email == null)
						$('#info_supplier_email').text('-');
					else
						$('#info_supplier_email').text(detailInfo.email);


					if (detailInfo.adress == null)
						$('#info_supplier_address').text('-');
					else
						$('#info_supplier_address').text(detailInfo.address);
				}
			})

		}

		// 填充货物详细信息
		function goodsInfoSet(goodsID) {
			var detailInfo;
			$.each(goodsCache, function (index, elem) {
				if (elem.id == goodsID) {
					detailInfo = elem;
					if (detailInfo.id == null)
						$('#info_goods_ID').text('-');
					else
						$('#info_goods_ID').text(detailInfo.id);

					if (detailInfo.name == null)
						$('#info_goods_name').text('-');
					else
						$('#info_goods_name').text(detailInfo.name);

					if (detailInfo.type == null)
						$('#info_goods_type').text('-');
					else
						$('#info_goods_type').text(detailInfo.type);

					if (detailInfo.size == null)
						$('#info_goods_size').text('-');
					else
						$('#info_goods_size').text(detailInfo.size);

					if (detailInfo.value == null)
						$('#info_goods_value').text('-');
					else
						$('#info_goods_value').text(detailInfo.value);
				}
			})
		}

		// 详细信息显示与隐藏
		function detilInfoToggle() {
			$('#info-show').click(function () {
				$('#detailInfo').removeClass('hide');
				$(this).addClass('hide');
				$('#info-hidden').removeClass('hide');
			});

			$('#info-hidden').click(function () {
				$('#detailInfo').removeClass('hide').addClass('hide');
				$(this).addClass('hide');
				$('#info-show').removeClass('hide');
			});
		}

		// 仓库下拉列表初始化
		function repositorySelectorInit() {
			$.ajax({
				type: 'GET',
				url: 'repositoryManage/getRepositoryList',
				dataType: 'json',
				contentType: 'application/json',
				data: {
					searchType: 'searchAll',
					keyWord: '',
					offset: -1,
					limit: -1
				},
				success: function (response) {
					$.each(response.rows, function (index, elem) {
						$('#repository_selector').append("<option value='" + elem.id + "'>" + elem.id + "号仓库</option>");
					});
				},
				error: function (response) {
					$('#repository_selector').append("<option value='-1'>加载失败</option>");
				}

			})
		}

		// 获取仓库当前库存量
		function fetchStorage() {
			$('#repository_selector').change(function () {
				stockin_repository = $(this).val();
				loadStorageInfo();
			});
		}

		function loadStorageInfo() {
			if (stockin_repository != null && stockin_goods != null) {
				$.ajax({
					type: 'GET',
					url: 'storageManage/getStorageListWithRepository',
					dataType: 'json',
					contentType: 'application/json',
					data: {
						offset: -1,
						limit: -1,
						searchType: 'searchByGoodsID',
						repositoryBelong: stockin_repository,
						keyword: stockin_goods
					},
					success: function (response) {
						if (response.total > 0) {
							data = response.rows[0].number;
							$('#info_storage').text(data);
						} else {
							$('#info_storage').text('0');
						}
					},
					error: function (response) {
						// do nothing
					}
				})
			}
		}

		// 执行货物入库操作
		function stockInOption() {
			$('#submit').click(function () {
				// data validate
				$('#stockin_form').data('bootstrapValidator').validate();
				if (!$('#stockin_form').data('bootstrapValidator').isValid()) {
					return;
				}

				data = {
					repositoryID: stockin_repository,
					supplierID: stockin_supplier,
					goodsID: stockin_goods,
					number: $('#stockin_input').val(),
				}

				$.ajax({
					type: 'POST',
					url: 'stockRecordManage/stockIn',
					dataType: 'json',
					content: 'application/json',
					data: data,
					success: function (response) {
						var msg;
						var type;
						var append = '';
						if (response.result == "success") {
							type = 'success';
							msg = '货物入库成功';
							inputReset();
						} else {
							type = 'error';
							msg = '货物入库失败'
						}
						showMsg(type, msg, append);
					},
					error: function (xhr, textStatus, errorThrown) {
						// handler error
						handleAjaxError(xhr.status);
					}
				})
			});
		}

		// 页面重置
		function inputReset() {
			$('#supplier_input').val('');
			$('#goods_input').val('');
			$('#stockin_input').val('');
			$('#info_supplier_ID').text('-');
			$('#info_supplier_name').text('-');
			$('#info_supplier_tel').text('-');
			$('#info_supplier_address').text('-');
			$('#info_supplier_email').text('-');
			$('#info_supplier_person').text('-');
			$('#info_goods_ID').text('-');
			$('#info_goods_name').text('-');
			$('#info_goods_size').text('-');
			$('#info_goods_type').text('-');
			$('#info_goods_value').text('-');
			$('#info_storage').text('-');
			$('#stockin_form').bootstrapValidator("resetForm", true);
		}

	</script>


	<div class="panel panel-default">
		<ol class="breadcrumb">
			<li>货物入库</li>
		</ol>
		<div class="panel-body">
			<div class="row">
				<div class="col-md-6 col-sm-6">
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-10 col-sm-11">
							<form action="" class="form-inline">
								<div class="form-group">
									<label for="" class="form-label">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;供应商：</label>
									<input type="text" class="form-control" id="supplier_input" placeholder="请输入供应商名称">
								</div>
							</form>
						</div>
					</div>
				</div>
				<div class="col-md-6 col-sm-6">
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-10 col-sm-11">
							<form action="" class="form-inline">
								<div class="form-group">
									<label for="" class="form-label">入库货物：</label>
									<input type="text" class="form-control" id="goods_input" placeholder="请输入货物名称">
								</div>
							</form>
						</div>
					</div>
				</div>
			</div>
			<div class="row visible-md visible-lg">
				<div class="col-md-12 col-sm-12">
					<div class='pull-right' style="cursor:pointer" id="info-show">
						<span>显示详细信息</span>
						<span class="glyphicon glyphicon-chevron-down"></span>
					</div>
					<div class='pull-right hide' style="cursor:pointer" id="info-hidden">
						<span>隐藏详细信息</span>
						<span class="glyphicon glyphicon-chevron-up"></span>
					</div>
				</div>
			</div>
			<div class="row hide" id="detailInfo" style="margin-bottom:30px">
				<div class="col-md-6 col-sm-6  visible-md visible-lg">
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-10 col-sm-10">
							<label for="" class="text-info">供应商信息</label>
						</div>
					</div>
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-11 col-sm-11">

							<table class="table table-bordered " data-toggle="table" id="tablesupplier" data-show-columns="true">
								<thead>
									<tr>
										<th>供应商ID</th>
										<th>负责人</th>
										<th>电子邮件</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<td>
											<span id="info_supplier_ID">-</span>
										</td>
										<td>
											<span id="info_supplier_person">-</span>
										</td>
										<td>
											<span id="info_supplier_email">-</span>
										</td>
									</tr>
								</tbody>
							</table>
							<table class="table table-bordered " data-toggle="table" id="tablesupplier2" data-show-columns="true">
								<thead>
									<tr>
										<th>供应商名</th>
										<th>联系电话</th>
										<th>联系地址</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<td>
											<span id="info_supplier_name">-</span>
										</td>
										<td>
											<span id="info_supplier_tel">-</span>
										</td>
										<td>
											<span id="info_supplier_address">-</span>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
				</div>
				<div class="col-md-6 col-sm-6">
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-11 col-sm-11">
							<label for="" class="text-info">货物信息</label>
						</div>
					</div>
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-11 col-sm-11">
							<table class="table table-bordered " data-toggle="table" id="goodssupplier" data-show-columns="true">
								<thead>
									<tr>
										<th>货物ID</th>
										<th>货物类型</th>
										<th>货物名</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<td>
											<span id="info_goods_ID">-</span>
										</td>
										<td>
											<span id="info_goods_type">-</span>
										</td>
										<td>
											<span id="info_goods_name">-</span>
										</td>
									</tr>
								</tbody>
							</table>
							<table class="table table-bordered " data-toggle="table" id="goodssupplier1" data-show-columns="true">
								<thead>
									<tr>
										<th>货物规格</th>
										<th>货物价值</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<td>
											<span id="info_goods_size">-</span>
										</td>
										<td>
											<span id="info_goods_value">-</span>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
			<div class="row" style="margin-top: 10px">
				<div class="col-md-6 col-sm-6">
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-10 col-sm-11">
							<form action="" class="form-inline">
								<div class="form-group">
									<label for="" class="form-label">入库仓库：</label>
									<select name="" id="repository_selector" class="form-control">
									</select>
								</div>
							</form>
						</div>
					</div>
				</div>
			</div>
			<div class="row" style="margin-top:20px">
				<div class="col-md-6 col-sm-6">
					<div class="row">
						<div class="col-md-1 col-sm-1"></div>
						<div class="col-md-10 col-sm-11">
							<form action="" class="form-inline" id="stockin_form">
								<div class="form-group">
									<label for="" class="control-label">入库数量：</label>
									<input type="text" class="form-control" placeholder="请输入数量" id="stockin_input" name="stockin_input">
									<span>(当前库存量：</span>
									<span id="info_storage">-</span>
									<span>)</span>
								</div>
							</form>
						</div>
					</div>
				</div>
			</div>
			<div class="row" style="margin-top:80px"></div>
		</div>
		<div class="panel-footer">
			<div style="text-align:right">
				<button class="btn btn-success" id="submit">提交入库</button>
			</div>
		</div>
	</div>