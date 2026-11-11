import unittest
from check_upgrade_live import check


class UpgradeLiveGateTests(unittest.TestCase):
    def setUp(self):
        self.case = {'upgrade': {'budgetVnd': 5_000_000}}
        self.line = dict(category='ram', productName='RAM 32GB (2x16GB) DDR5', quantity=2,
                         name='New memory', priceMinVnd=4_000_000, priceMaxVnd=4_000_000,
                         priceChecked=True, inStock=True, sourceUrl='https://example.com/ram')
        self.expected = dict(require=['ram'], ramType='DDR5', maxRamSticks=2,
                             oldRamSticks=2, oldRamGb=16, minRamGb=32)

    def result(self, **changes):
        return {'reply': 'Replace the old RAM with this kit.', 'build': {'items': [self.line | changes]}}

    def test_two_module_replacement_within_budget(self):
        self.assertEqual(check(self.case, self.result(), self.expected)['issues'], [])

    def test_third_module_cannot_fit_two_occupied_slots(self):
        result = self.result(productName='RAM 16GB DDR5', name='Extra memory', quantity=1)
        self.assertIn('RAM plan exceeds physical slot count', check(self.case, result, self.expected)['issues'])

    def test_capacity_downgrade(self):
        result = self.result(productName='RAM 16GB DDR5', quantity=1)
        self.assertIn('RAM plan does not reach required capacity', check(self.case, result, self.expected)['issues'])

    def test_stock_unknown_is_not_a_verified_purchase(self):
        self.assertTrue(check(self.case, self.result(inStock=None), self.expected)['issues'])

    def test_over_budget_even_if_backend_claims_price_checked(self):
        self.assertIn('Exceeded upgrade budget', check(self.case, self.result(priceMaxVnd=6_000_000), self.expected)['issues'])

    def test_cannot_pass_when_useful_plan_is_missing(self):
        self.assertTrue(check(self.case, {'reply': 'No result'}, self.expected)['issues'])

    def test_refusal_allowed_only_when_case_explicitly_allows_it(self):
        self.assertEqual(check(self.case, {'reply': 'No result'}, self.expected | {'allowNoCard': True})['issues'], [])


if __name__ == '__main__':
    unittest.main()
