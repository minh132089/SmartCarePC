"""Các bẫy có thể làm điểm nghiệm thu cao giả; không tính các test này vào 100 ca API."""
import unittest
import subprocess
import sys
from pathlib import Path
import tempfile
from unittest.mock import patch
from urllib.error import URLError
from capture_advice_prices import direct_evidence
from assemble_advice_gold import choose_price_label, stamp
from evaluate_advice import acceptance_passed, evaluate_case, evaluate_prices, gpu_model, gpu_vendor, ram_shape


class EvaluationTests(unittest.TestCase):

    def test_fury32_is_a_two_module_kit_even_if_seo_title_hides_sku(self):
        url='https://fptshop.com.vn/linh-kien/ram-kingston-fury-32-gb-ddr4-3200-mhz'
        self.assertEqual(ram_shape('RAM Kingston Fury 32 GB-DDR4-3200 MHz',url),(2,16))
        self.assertEqual(ram_shape('RAM 32GB KF432C16BB1AK2/32'),(2,16))
        self.assertEqual(ram_shape('RAM 32GB KF432C16BB/32'),(1,32))
        line={'category':'ram','productName':'RAM Kingston Fury 32 GB-DDR4-3200 MHz','quantity':2,'sourceUrl':url}
        _,issues=evaluate_case({'expected':{'minRamGb':64}},{'options':[{'items':[line]}]})
        self.assertTrue(any('thiếu RAM' in issue for issue in issues))
        priced=dict(line,quantity=4,code='ram',priceMaxVnd=8780000,priceChecked=True,inStock=True,checkedAt='2026-10-23T08:00:00Z')
        label={'url':url,'title':'RAM 32GB KF432C16BB1AK2/32','unitPriceVnd':4390000,'reviewed':True,'capturedAt':'2026-10-23T08:01:00Z'}
        self.assertEqual(evaluate_prices({'id':'fury','prices':[label]},[{'items':[priced]}])[:2],(1,1))

    def test_sparkle_titan_cannot_use_small_gpu_model(self):
        line={'code':'gpu_240','category':'gpu','productName':'SPARKLE Intel Arc B580 TITAN OC SB580T-12GOC'}
        _,issues=evaluate_case({'expected':{}},{'options':[{'items':[line]}]})
        self.assertTrue(any('315mm' in issue for issue in issues))

    def test_three_fan_pack_costs_one_pack_and_spares_are_allowed(self):
        line={'code':'fan120_argb','category':'fans','productName':'Bộ 3 quạt Xigmatek STARLINK ARGB BLACK',
              'quantity':3,'priceMaxVnd':490000,'priceChecked':True,'inStock':True,'sourceUrl':'https://shop/3fans','checkedAt':'2026-10-23T08:00:00Z'}
        label={'url':line['sourceUrl'],'title':line['productName'],'unitPriceVnd':490000,'reviewed':True,'capturedAt':'2026-10-23T08:01:00Z'}
        for qty,price in [(1,490000),(3,490000),(4,980000),(6,980000)]:
            self.assertEqual(evaluate_prices({'id':'fans','prices':[label]},[{'items':[dict(line,quantity=qty,priceMaxVnd=price)]}])[:2],(1,1))
        self.assertEqual(evaluate_prices({'id':'fans','prices':[label]},[{'items':[dict(line,priceMaxVnd=1470000)]}])[:2],(0,1))

    def test_ai_warranty_claim_cannot_override_store_evidence(self):
        line={'category':'psu','productName':'JETEK B650 650W','spec':'650W Bronze, bảo hành 60 tháng','warrantyMonths':36}
        _,issues=evaluate_case({'expected':{}},{'options':[{'items':[line]}]})
        self.assertTrue(any('bảo hành' in issue for issue in issues))

    def test_h610m_e_cannot_hold_four_modules_even_when_compatibility_says_true(self):
        for title in ['Mainboard MSI PRO H610M-E DDR4 | LGA 1700, mATX, 2 khe RAM', 'MSI PRO B760M-E DDR4']:
            card={'tier':'cheap','compatible':True,'items':[
                {'category':'motherboard','productName':title},
                {'category':'ram','productName':'Lexar THOR 32GB (2x16GB) DDR4','quantity':4}]}
            _,issues=evaluate_case({'expected':{'minRamGb':64}},{'options':[card]})
            self.assertTrue(any('quá 2 khe RAM' in issue for issue in issues))

    def test_gskill_sku_uses_physical_modules_and_one_price_per_kit(self):
        title='RAM G.SKILL Trident Z5 32 GB-DDR5-6000 MHz (F5-6000U4040E16GX2-TZ5K)'
        self.assertEqual(ram_shape(title),(2,16))
        line={'code':'ram','category':'ram','productName':title,'quantity':2,'sourceUrl':'https://shop/kit',
              'priceMaxVnd':4199000,'priceChecked':True,'inStock':True,'checkedAt':'2026-10-23T08:00:00Z'}
        label={'url':'https://shop/kit','title':title,'unitPriceVnd':4199000,'reviewed':True,'capturedAt':'2026-10-23T08:01:00Z'}
        self.assertEqual(evaluate_prices({'id':'kit','prices':[label]},[{'items':[line]}])[:2],(1,1))
        bad=dict(line,quantity=1)
        self.assertEqual(evaluate_prices({'id':'kit','prices':[label]},[{'items':[bad]}])[:2],(0,1))
        _,issues=evaluate_case({'expected':{'minRamGb':32}},{'options':[{'items':[bad]}]})
        self.assertTrue(any('RAM' in issue for issue in issues))

    def test_independent_reader_retries_tls_failure_with_raw_primary_html(self):
        html=b'<h1>SSD Kingston NV3 1TB</h1><p>1.790.000 VND</p>'
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'page.html'
            with patch('capture_advice_prices.urllib.request.urlopen',side_effect=URLError('TLS reset')), \
                 patch('capture_advice_prices.subprocess.run',return_value=subprocess.CompletedProcess([],0,stdout=html)) as curl:
                title,price,_=direct_evidence('https://shop/ssd',path)
            self.assertEqual(title,'SSD Kingston NV3 1TB')
            self.assertEqual(price,1790000)
            self.assertEqual(path.read_bytes(),html)
            self.assertIn('--fail',curl.call_args.args[0])

    def test_failed_primary_retry_does_not_create_price_evidence(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'page.html'
            with patch('capture_advice_prices.urllib.request.urlopen',side_effect=URLError('TLS reset')), \
                 patch('capture_advice_prices.subprocess.run',side_effect=subprocess.CalledProcessError(22,['curl'])):
                with self.assertRaises(subprocess.CalledProcessError):direct_evidence('https://shop/ssd',path)
            self.assertFalse(path.exists())

    def test_trademark_symbols_do_not_change_gpu_model_or_hide_variants(self):
        for title in ['ASUS Dual GeForce RTX™ 4070 - 12GB GDDR6X', 'RTX® 4070 12GB']:
            self.assertEqual(gpu_model(title), 'rtx4070')
            self.assertEqual(gpu_vendor(title), 'nvidia')
        self.assertEqual(gpu_model('RTX™ 4070 Ti SUPER 16GB'), 'rtx4070tisuper')
        self.assertNotEqual(gpu_model('RTX™ 4070 Ti 12GB'), 'rtx4070')
        self.assertNotEqual(gpu_model('RTX® 4060 8GB'), 'rtx4070')

    def test_failed_fetch_does_not_displace_a_real_price_observation(self):
        with tempfile.TemporaryDirectory() as folder:
            page=Path(folder)/'page.html';page.write_text('<h1>GPU</h1><p>9000000 VND</p>')
            failed={'url':'https://shop/gpu','capturedAt':'2026-10-23T08:00:00Z','unitPriceVnd':None}
            real=dict(failed,capturedAt='2026-10-23T08:00:30Z',unitPriceVnd=9000000,title='GPU',snapshot=str(page))
            self.assertIs(choose_price_label([failed,real],failed['url'],[stamp(failed['capturedAt'])]),real)
            self.assertIs(choose_price_label([failed],failed['url'],[stamp(failed['capturedAt'])]),failed)

    def test_readable_wrong_price_cannot_be_skipped_for_a_matching_price(self):
        with tempfile.TemporaryDirectory() as folder:
            page=Path(folder)/'page.html';page.write_text('source')
            close={'url':'https://shop/gpu','capturedAt':'2026-10-23T08:00:10Z','unitPriceVnd':8000000,'title':'GPU','snapshot':str(page)}
            farther=dict(close,capturedAt='2026-10-23T08:00:30Z',unitPriceVnd=9000000)
            self.assertIs(choose_price_label([farther,close],close['url'],[stamp('2026-10-23T08:00:00Z')]),close)

    def test_a620am_is_am5_and_cannot_pair_with_intel(self):
        _, issues = evaluate_case({'expected': {}}, {'options': [{'items': [
            {'category': 'cpu', 'productName': 'CPU Intel Core i5 14400F'},
            {'category': 'motherboard', 'productName': 'MSI PRO A620AM-B EVO'}]}]})
        self.assertTrue(any('CPU/bo khác nền tảng' in issue for issue in issues))

    def test_grader_cli_accepts_zero_issue_option(self):
        result = subprocess.run([sys.executable, '-B', str(Path(__file__).with_name('evaluate_advice.py')), '--help'], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('--zero-issues', result.stdout)

    def test_zero_issue_gate_rejects_any_failed_metric_and_insufficient_samples(self):
        self.assertTrue(acceptance_passed(120, 235, 1, 1, 1, 1))
        for decision, advice, price in [(118/120, 1, 1), (1, 119/120, 1), (1, 1, 234/235)]:
            self.assertFalse(acceptance_passed(120, 235, decision, advice, price, 1))
        self.assertFalse(acceptance_passed(99, 235, 1, 1, 1, 1))
        self.assertFalse(acceptance_passed(120, 99, 1, 1, 1, 1))

    def test_only_an_overbudget_quality_tier_is_not_a_solved_purchase(self):
        card={'tier':'quality','items':[{'priceMaxVnd':13000000}]}
        _,issues=evaluate_case({'expected':{'budgetVnd':11000000}},{'options':[card]})
        self.assertIn('không có phương án nào trong ngân sách gốc',issues)

    def test_laptop_pad_cannot_be_a_desktop_cpu_cooler(self):
        card={'tier':'quality','items':[{'code':'tower_air_155','category':'cooler','productName':'Đế Tản Nhiệt Cooler Master X-Slim II','quantity':1}]}
        _,issues=evaluate_case({'expected':{}},{'options':[card]})
        self.assertTrue(any('đế tản laptop' in issue for issue in issues))
    def test_generic_apology_is_not_valid_unavailability(self):
        _, issues=evaluate_case({'expected':{'allowUnavailable':True}}, {'reply':'Xin lỗi, mình chưa trả lời được câu này. Bạn hỏi lại nhé.'})
        self.assertTrue(issues)

    def test_actual_part_shortage_is_distinct(self):
        _, issues=evaluate_case({'expected':{'allowUnavailable':True}}, {'reply':'Phương án Phù hợp: chưa tìm được link mua còn hàng cho RAM DDR4 32GB trong mức tối đa 20 triệu.'})
        self.assertFalse(issues)

    def test_price_needs_review_and_exact_same_time_value(self):
        line={'code':'gpu','category':'gpu','quantity':1,'sourceUrl':'https://shop/gpu','productName':'RTX 4060 8GB',
              'priceMaxVnd':9000000,'priceChecked':True,'inStock':True,'checkedAt':'2026-10-23T08:00:00Z'}
        label={'url':'https://shop/gpu','title':'RTX 4060 8GB','unitPriceVnd':9000000,'capturedAt':'2026-10-23T08:01:00Z','reviewed':True}
        gold={'id':'a','prices':[label]};cards=[{'items':[line]}]
        self.assertEqual(evaluate_prices(gold,cards)[:2],(1,1))
        for change in ({'reviewed':False},{'unitPriceVnd':8999999},{'capturedAt':'2026-10-23T08:11:00Z'}):
            with self.subTest(change=change):
                self.assertEqual(evaluate_prices({'id':'a','prices':[dict(label,**change)]},cards)[:2],(0,1))

    def test_ram_kit_price_is_per_pack(self):
        line={'code':'ram','category':'ram','quantity':2,'sourceUrl':'https://shop/ram','productName':'RAM 2x16GB DDR4',
              'priceMaxVnd':6000000,'priceChecked':True,'inStock':True,'checkedAt':'2026-10-23T08:00:00Z'}
        label={'url':'https://shop/ram','title':'RAM 2x16GB DDR4','unitPriceVnd':6000000,'capturedAt':'2026-10-23T08:01:00Z','reviewed':True}
        self.assertEqual(evaluate_prices({'id':'kit','prices':[label]},[{'items':[line]}])[:2],(1,1))

    def test_reused_capture_does_not_hide_a_later_wrong_line(self):
        line={'code':'gpu','category':'gpu','quantity':1,'sourceUrl':'https://shop/gpu','productName':'RTX 4060 8GB',
              'priceMaxVnd':9000000,'priceChecked':True,'inStock':True,'checkedAt':'2026-10-23T08:00:00Z'}
        label={'url':'https://shop/gpu','title':'RTX 4060 8GB','unitPriceVnd':9000000,'capturedAt':'2026-10-23T08:01:00Z','reviewed':True}
        gold={'id':'a','prices':[label]};seen={}
        self.assertEqual(evaluate_prices(gold,[{'items':[line]}],seen)[:2],(1,1))
        # Trừ lại quan sát đã tính đạt; không cộng mẫu thứ hai cho cùng trang/lần chụp.
        self.assertEqual(evaluate_prices(gold,[{'items':[dict(line,priceMaxVnd=8999999)]}],seen)[:2],(-1,0))
        self.assertEqual(evaluate_prices(gold,[{'items':[line]}],seen)[:2],(0,0))


if __name__=='__main__':unittest.main()
